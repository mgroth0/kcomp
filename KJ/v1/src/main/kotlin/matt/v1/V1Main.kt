package matt.v1

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.StringConverter
import matt.gui.app.GuiApp
import matt.gui.core.context.mcontextmenu
import matt.gui.loop.runLater
import matt.gui.loop.runLaterReturn
import matt.gui.resize.DragResizer
import matt.hurricanefx.dragsSnapshot
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.times
import matt.hurricanefx.lazyTab
import matt.hurricanefx.op
import matt.hurricanefx.tornadofx.control.button
import matt.hurricanefx.tornadofx.fx.attachTo
import matt.hurricanefx.tornadofx.layout.vbox
import matt.hurricanefx.tornadofx.nodes.disableWhen
import matt.hurricanefx.tornadofx.nodes.vgrow
import matt.hurricanefx.tornadofx.tab.staticTab
import matt.hurricanefx.tornadofx.tab.tabpane
import matt.hurricanefx.visibleAndManagedProp
import matt.kjlib.async.daemon
import matt.kjlib.log.NEVER
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.cap
import matt.reflect.ismac
import matt.reflect.onLinux
import matt.remote.host.Hosts
import matt.remote.runThisOnOM
import matt.remote.slurm.SRun
import matt.v1.STARTUP.ITTI_KOCH
import matt.v1.exps.experiments
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.gui.StatusLabel.Status.IDLE
import matt.v1.gui.StatusLabel.Status.WORKING
import matt.v1.lab.ExpCategory
import matt.v1.vis.IttiKochVisualizer
import matt.v1.vis.IttiKochVisualizer.Companion.dogFolder
import matt.v1.vis.IttiKochVisualizer.Companion.ittiKochInput
import matt.v1.vis.RosenbergVisualizer
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private enum class STARTUP { ROSENBERG, ITTI_KOCH }

private val startup: STARTUP = STARTUP.ROSENBERG
private const val REMOTE = false
private val REMOTE_AND_MAC = REMOTE && ismac

fun main(): Unit = GuiApp {
  val remoteStatus = if (REMOTE_AND_MAC) StatusLabel("remote") else null
  if (REMOTE_AND_MAC) {
	thread {
	  remoteStatus!!.status.value = WORKING
	  Hosts.POLESTAR.ssh(object: Appendable {
		var clearOnNext = false
		override fun append(csq: CharSequence?): java.lang.Appendable {
		  csq?.forEach {
			append(it)
		  }
		  return this
		}

		override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
		  if (csq != null) append(csq.subSequence(start, end))
		  return this
		}

		override fun append(c: Char): java.lang.Appendable {
		  if (c in listOf('\n', '\r')) {
			clearOnNext = true
		  } else {
			if (clearOnNext) {
			  remoteStatus.statusExtra = ""
			  clearOnNext = false
			}
			remoteStatus.statusExtra += c
		  }
		  return this
		}

	  }) {
		runThisOnOM(srun = SRun(timeMin = 15))
	  }
	  remoteStatus.status.value = IDLE
	}
  }
  onLinux {
	println("hello from linux!")
	exitProcess(0)
  }



  vbox {
	alignment = Pos.CENTER
	val visualizer = tabpane {
	  this.vgrow = ALWAYS
	  tabs += lazyTab("Rosenberg") { RosenbergVisualizer() }
	  tabs += lazyTab("Itti Koch") {
		VBox(
		  ComboBox(
			dogFolder.listFiles()!!.sorted().toObservable()
		  ).apply {
			valueProperty().bindBidirectional(ittiKochInput)
			/*simpleCellFactory { it.name to null }*/
			this.converter = object: StringConverter<File>() {
			  override fun toString(`object`: File?): String {
				return `object`!!.name
			  }

			  override fun fromString(string: String?): File {
				NEVER
			  }

			}
		  }, IttiKochVisualizer()
		).apply { alignment = Pos.CENTER }
	  }
	  when (startup) {
		STARTUP.ROSENBERG -> selectionModel.select(tabs.first())
		ITTI_KOCH         -> selectionModel.select(tabs.last())
	  }

	  DragResizer.makeResizable(this)
	  border = Border(
		BorderStroke(
		  null, null, Color.GRAY, null, null, null, BorderStrokeStyle.SOLID, null, null,
		  BorderWidths(0.0, 0.0, DragResizer.RESIZE_MARGIN.toDouble(), 0.0), null
		)
	  )
	}

	val ittKochTab = visualizer.tabs[1]

	val loadProp = BProp(false)
	val expBox = vbox {
	  this.vgrow = ALWAYS
	  exactHeightProperty().bind(stage.heightProperty()*0.75)
	  mcontextmenu {
		checkitem("load", loadProp)
	  }
	  visibleAndManagedProp().bind(ittKochTab.selectedProperty().not())
	}

	val figButtonBox = expBox.tabpane {
	  ExpCategory.values().forEach {
		staticTab(it.name.lowercase().cap(), it.pane)
	  }
	  exactHeight = 120.0
	}
	val fig = Figure().attachTo(expBox)
	val statusLabel = StatusLabel().attachTo(expBox)
	fig.vgrow = ALWAYS
	/*fig.exactHeightProperty()
		.bind(
		  expBox.heightProperty()
		  - figButtonBox.heightProperty()
		  - statusLabel.heightProperty()
		)*/
	fig.dragsSnapshot()
	val exps = experiments(fig, statusLabel)
	val allButtons = mutableListOf<Button>()
	exps.forEach { exp ->
	  exp.category.pane.button(exp.name.addSpacesUntilLengthIs(4)) {
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
			true -> {
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
	if (REMOTE_AND_MAC) {
	  remoteStatus!!.attachTo(this)
	}
  }
}.start(
  shutdown = {
	println("debug shutdown")
  }
)






