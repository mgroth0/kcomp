package matt.v1

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.FlowPane
import javafx.scene.paint.Color
import matt.gui.app.GuiApp
import matt.gui.core.context.mcontextmenu
import matt.gui.loop.runLater
import matt.gui.loop.runLaterReturn
import matt.gui.proto.scaledCanvas
import matt.gui.resize.DragResizer
import matt.gui.resize.DragResizer.Companion.RESIZE_MARGIN
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.minus
import matt.hurricanefx.op
import matt.hurricanefx.tornadofx.control.button
import matt.hurricanefx.tornadofx.fx.attachTo
import matt.hurricanefx.tornadofx.layout.vbox
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.disableWhen
import matt.hurricanefx.tornadofx.tab.staticTab
import matt.hurricanefx.tornadofx.tab.tabpane
import matt.kjlib.async.daemon
import matt.kjlib.commons.ismac
import matt.kjlib.date.globaltic
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.stream.forEachNested
import matt.v1.Status.IDLE
import matt.v1.Status.WORKING
import matt.v1.exps.experiments
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.gui.VisualizationCfg
import matt.v1.lab.ExpCategory.OTHER
import matt.v1.lab.ExpCategory.ROSENBERG
import matt.v1.lab.THETA_MAX
import matt.v1.lab.THETA_MIN
import matt.v1.lab.allSimpleCosCells
import matt.v1.lab.baseSimpleSinCell
import matt.v1.lab.baseStim
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import kotlin.system.exitProcess


enum class Status { WORKING, IDLE }


const val VISUAL_SCALE = 3.0
const val SAMPLE_HW = 100

fun main(): Unit = GuiApp {
  if (!ismac()) {
	println("hello from linux!")
	exitProcess(0)
  }
  globaltic()
  vbox {
	alignment = Pos.CENTER

	val stimVisualizerBox = vbox {
	  DragResizer.makeResizable(this)
	  alignment = Pos.CENTER
	  border = Border(
		BorderStroke(
		  null, null, Color.GRAY, null, null, null, BorderStrokeStyle.SOLID, null, null,
		  BorderWidths(0.0, 0.0, RESIZE_MARGIN.toDouble(), 0.0), null
		)
	  )
	}

	val canv = stimVisualizerBox.scaledCanvas(hw = SAMPLE_HW, scale = VISUAL_SCALE)

	fun show(stim: Stimulus? = null, cell: SimpleCell? = null) =
		(0..SAMPLE_HW)
			.forEachNested { x, y ->
			  val rg = stim?.getVisSample(x/SAMPLE_HW.toDouble(), y/SAMPLE_HW.toDouble()) ?: 0.0
			  val b = cell?.getVisSample(x/SAMPLE_HW.toDouble(), y/SAMPLE_HW.toDouble()) ?: 0.0
			  canv[x, y] = Color.color(rg, rg, b)
			}

	val cfg = object: VisualizationCfg(responsive = true) {
	  var stimulus by CfgObjProp(baseStim.copy(f = baseStim.f.copy(X0 = 0.0)), null)
	  var cell by CfgObjProp(
		null,
		baseSimpleSinCell.copy(f = baseSimpleSinCell.f.copy(X0 = 0.0)),
		allSimpleCosCells[0].copy(f = baseSimpleSinCell.f.copy(X0 = 0.0))
	  )
	  val SF by CfgDoubleProp(0.01 to 1)
	  val theta by CfgDoubleProp(THETA_MIN to THETA_MAX)
	  val sigmaMult by CfgDoubleProp(0.01 to 10)
	  val gaussian by CfgBoolProp(true)
	  override fun update() =
		  show(
			stim = stimulus?.copy(
			  SF = SF,
			  f = stimulus!!.f.copy(t = theta),
			  s = stimulus!!.s*sigmaMult,
			  gaussianEnveloped = gaussian
			),
			cell = cell?.copy(
			  SF = SF,
			  f = cell!!.f.copy(t = theta),
			  sx = cell!!.sx*sigmaMult,
			  sy = cell!!.sy*sigmaMult,
			  gaussianEnveloped = gaussian
			)
		  )
	}
	cfg.update()
	stimVisualizerBox.add(cfg.cfgPane.apply {
	  exactWidthProperty().bind(stage.widthProperty())
	})

	val loadProp = BProp(false)
	val expBox = vbox {
	  exactHeightProperty().bind(stage.heightProperty() - stimVisualizerBox.heightProperty())
	  mcontextmenu {
		checkitem("load", loadProp)
	  }
	}

	val rosenbergPane = FlowPane()
	val otherPane = FlowPane()
	val figButtonBox = expBox.tabpane {
	  staticTab("Rosenberg", rosenbergPane)
	  staticTab("Other", otherPane)
	  exactHeight = 120.0
	}
	val fig = Figure().attachTo(expBox)
	val statusLabel = StatusLabel().attachTo(expBox)
	fig.exactHeightProperty()
		.bind(
		  expBox.heightProperty()
		  - figButtonBox.heightProperty()
		  - statusLabel.heightProperty()
		)
	val exps = experiments(fig, statusLabel)
	val allButtons = mutableListOf<Button>()
	exps.forEach { exp ->
	  when (exp.category) {
		ROSENBERG -> rosenbergPane
		OTHER     -> otherPane
	  }.button(exp.name.addSpacesUntilLengthIs(4)) {
		allButtons += this
		fun cfgStartButton() {
		  text = exp.name.addSpacesUntilLengthIs(4)
		  setOnAction {
			styleClass += "greenBackground"
			allButtons.filter { it != this }.forEach {
			  it.styleClass -= "greenBackground"
			}
			statusLabel.status.value = WORKING
			exp.setupFig()
			daemon {
			  if (loadProp.value) {
				runLater {
				  exp.load()
				}
			  } else exp.run()
			  runLaterReturn {
				statusLabel.status.value = IDLE
			  }
			}
		  }
		}

		exp.runningProp.onChange {
		  when (it) {
			true  -> {
			  text = "stop"
			  op = exp::stop
			}
			false -> cfgStartButton()
		  }
		}
		cfgStartButton()


	  }.also {
		it.disableWhen { statusLabel.status.isEqualTo(WORKING).and(exp.runningProp.not()) }
	  }
	}
  }
}.start()





