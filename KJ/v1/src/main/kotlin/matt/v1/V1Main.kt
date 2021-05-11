package matt.v1

import javafx.geometry.Pos
import javafx.scene.paint.Color
import matt.gui.app.GuiApp
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.div
import matt.hurricanefx.eye.prop.minus
import matt.hurricanefx.eye.prop.times
import matt.hurricanefx.op
import matt.hurricanefx.tornadofx.control.button
import matt.hurricanefx.tornadofx.fx.attachTo
import matt.hurricanefx.tornadofx.layout.canvas
import matt.hurricanefx.tornadofx.layout.hbox
import matt.hurricanefx.tornadofx.layout.vbox
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.disableWhen
import matt.kjlib.async.daemon
import matt.kjlib.date.globaltic
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.stream.forEachNested
import matt.v1.Status.IDLE
import matt.v1.Status.WORKING
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.gui.VisualizationCfg
import matt.v1.lab.THETA_MAX
import matt.v1.lab.THETA_MIN
import matt.v1.lab.allSimpleCosCells
import matt.v1.lab.baseSimpleSinCell
import matt.v1.lab.baseStim
import matt.v1.lab.experiments
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus


enum class Status { WORKING, IDLE }


const val VISUAL_SCALE = 3.0
const val SAMPLE_HW = 100

fun main(): Unit = GuiApp {
  globaltic()
  vbox {
	alignment = Pos.CENTER
	val canv = canvas(SAMPLE_HW.toDouble(), SAMPLE_HW.toDouble()) {
	  this.scaleX = VISUAL_SCALE
	  this.scaleY = VISUAL_SCALE
	  translateYProperty().bind(scaleYProperty().minus(1).times(-1)*heightProperty()/2)
	}
	this.vbox { prefHeight = 25.0 }
	val gc = canv.graphicsContext2D
	val pw = gc.pixelWriter


	fun show(stim: Stimulus? = null, cell: SimpleCell? = null) =
		(0..SAMPLE_HW)
			.forEachNested { x, y ->
			  val rg = stim?.getVisSample(x/SAMPLE_HW.toDouble(), y/SAMPLE_HW.toDouble()) ?: 0.0
			  val b = cell?.getVisSample(x/SAMPLE_HW.toDouble(), y/SAMPLE_HW.toDouble()) ?: 0.0
			  pw.setColor(x, y, Color.color(rg, rg, b))
			}

	val cfg = object: VisualizationCfg(responsive = true) {
	  var stimulus by CfgObjProp(baseStim.copy(f = baseStim.f.copy(X0 = 0.0)), null)
	  var cell by CfgObjProp(null, baseSimpleSinCell.copy(f = baseSimpleSinCell.f.copy(X0 = 0.0)), allSimpleCosCells[0].copy(f = baseSimpleSinCell.f.copy(X0 = 0.0)))
	  val SF by CfgDoubleProp(0.01 to 1)
	  val theta by CfgDoubleProp(THETA_MIN to THETA_MAX)
	  val sigmaMult by CfgDoubleProp(0.01 to 10)
	  override fun update() =
		  show(
			stim = stimulus?.copy(
			  SF = SF,
			  f = stimulus!!.f.copy(t = theta),
			  s = stimulus!!.s*sigmaMult
			),
			cell = cell?.copy(
			  SF = SF,
			  f = cell!!.f.copy(t = theta),
			  sx = cell!!.sx*sigmaMult,
			  sy = cell!!.sy*sigmaMult
			)
		  )
	}
	cfg.update()
	add(cfg.cfgPane.apply {
	  exactWidthProperty().bind(stage.widthProperty())
	})

	val figButtonBox = hbox()

	val fig = Figure().attachTo(this)
	val statusLabel = StatusLabel().attachTo(this)
	val exps = experiments(fig, statusLabel)
	exps.forEach { exp ->
	  figButtonBox.button(exp.name.addSpacesUntilLengthIs(4)) {

		fun cfgStartButton() {
		  text = exp.name.addSpacesUntilLengthIs(4)
		  setOnAction {
			statusLabel.status.value = WORKING
			exp.setupFig()
			daemon {
			  exp.run()
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





