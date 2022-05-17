package matt.v1

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.StringConverter
import matt.gui.app.GuiApp
import matt.gui.core.context.mcontextmenu
import matt.gui.loop.runLaterReturn
import matt.gui.resize.DragResizer
import matt.hurricanefx.dragsSnapshot
import matt.hurricanefx.exactHeight
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.doubleBinding
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
import matt.kjlib.date.tic
import matt.kjlib.file.text
import matt.kjlib.log.NEVER
import matt.kjlib.log.err
import matt.kjlib.recurse.recurse
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.cap
import matt.reflect.ismac
import matt.reflect.onLinux
import matt.remote.host.Hosts
import matt.remote.runThisOnOM
import matt.remote.slurm.SRun
import matt.v1.STARTUP.ITTI_KOCH
import matt.v1.exps.experiments
import matt.v1.exps.expmodels.ExpCategory
import matt.v1.gui.ExpGui
import matt.v1.gui.fig.Figure
import matt.v1.gui.status.StatusLabel
import matt.v1.gui.status.StatusLabel.Status.IDLE
import matt.v1.gui.status.StatusLabel.Status.WORKING
import matt.v1.gui.vis.IttiKochVisualizer
import matt.v1.gui.vis.IttiKochVisualizer.Companion.dogFolder
import matt.v1.gui.vis.IttiKochVisualizer.Companion.ittiKochInput
import matt.v1.gui.vis.RosenbergVisualizer
import matt.v1.lab.petri.popLouieFullThetaCells
import matt.v1.model.combined.ARI_BASE_CFG
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private enum class STARTUP { ROSENBERG, ITTI_KOCH }


private val startup: STARTUP = STARTUP.ROSENBERG
private const val REMOTE = false
private val REMOTE_AND_MAC = REMOTE && ismac
val squareFigProp = SimpleBooleanProperty(true)

val latestPop by lazy {
  popLouieFullThetaCells.copy(
	cellX0AbsMinmax = 15.0,
	cellX0Step = 5.0,
	cellPrefThetaStep = 15.0,
	reqSize = null
  )
}

val visualizer by lazy { RosenbergVisualizer(ARI_BASE_CFG) }

fun main(): Unit = GuiApp(screenIndex = 2) {


  /*simplePrinting = true*/
  val t = tic(prefix = "V1Main")
  t.toc("top of V1Main main()")

  thread {
	File("/Users/matthewgroth/registered/kcomp/KJ")
	  .recurse { it.listFiles()?.toList() ?: listOf() }
	  .filter { it.extension == "kt" && it.absolutePath != "/Users/matthewgroth/registered/kcomp/KJ/v1/src/main/kotlin/matt/v1/V1Main.kt" }
	  .forEach {
		if ("ApfloatMath.euler" in it.text) {
		  err("ApfloatMath.euler is NOT what you think it is")
		}
	  }

  }


  /*NativeLoader.load()

  Logger.getLogger(OpenCLLoader::class.qualifiedName).level = Level.INFO
  println("OpenCLLoader.isOpenCLAvailable():${OpenCLLoader.isOpenCLAvailable()}")
  *//*aparAPITest()*//*
  val r = kernel(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)).calc()
  println("kernel result: ${r}")*/

  val remoteStatus = if (REMOTE_AND_MAC) StatusLabel("remote") else null
  if (REMOTE_AND_MAC) {
	thread {
	  remoteStatus!!.status.value = WORKING
	  println("WORKING 2")
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
	  println("IDLE 2")
	}
  }
  onLinux {
	println("hello from linux!")
	exitProcess(0)
  }


  t.toc("doing gui stuff")
  rootVbox {
	alignment = TOP_CENTER
	/*val figHeightProp = DProp(500.0)*/
	val visualizer = tabpane {
	  tabs += lazyTab("Rosenberg") { visualizer.node }
	  tabs += lazyTab("Itti Koch") {
		this.isDisable = true /*it throws an error and I dont have time for this right now*/
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
		  }, IttiKochVisualizer().node
		).apply { alignment = Pos.CENTER }
	  }
	  when (startup) {
		STARTUP.ROSENBERG -> selectionModel.select(tabs.first())
		ITTI_KOCH         -> selectionModel.select(tabs.last())
	  }

	  /*DragResizer.makeResizable(this) {
		figHeightProp.value = kotlin.math.max(kotlin.math.min(figHeightProp.value - it, 600.0), 300.0)
	  }*/
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
	  alignment = TOP_CENTER
	  this.vgrow = ALWAYS
	  /*exactHeightProperty().bind(figHeightProp)*/
	  mcontextmenu {
		checkitem("load", loadProp)
		"toggle square fig" toggles squareFigProp
	  }
	  visibleAndManagedProp().bind(ittKochTab.selectedProperty().not())
	}

	val expCatPanes by lazy {
	  ExpCategory.values().associateWith { FlowPane() }
	}

	expBox.tabpane {
	  this.vgrow = ALWAYS
	  ExpCategory.values().forEach {
		staticTab(it.name.lowercase().cap(), expCatPanes[it]!!)
	  }
	  exactHeight = 120.0
	}
	val fig = Figure().attachTo(expBox)
	val statusLabel = StatusLabel().attachTo(expBox)
	fig.maxWidthProperty().bind(squareFigProp.doubleBinding(fig.heightProperty()) {
	  if (it!!) fig.height else Double.MAX_VALUE
	})
	fig.vgrow = ALWAYS
	fig.dragsSnapshot()
	val allButtons = mutableListOf<Button>()
	experiments().forEach { exp ->
	  expCatPanes[exp.category]!!.button(exp.name.addSpacesUntilLengthIs(4)) {
		allButtons += this
		fun cfgStartButton() {
		  text = exp.name.addSpacesUntilLengthIs(4)
		  setOnAction {
			styleClass += "greenBackground"
			allButtons.filter { it != this }.forEach {
			  it.styleClass -= "greenBackground"
			}
			statusLabel.counters.clear()
			statusLabel.status.value = WORKING
			println("WORKING 1")
			fig.clear()
			fig.setup(
			  chartTitle = exp.title,
			  xAxisConfig = exp.xAxisConfig,
			  yAxisConfig = exp.yAxisConfig,
			  seriesCfgs = exp.series,
			)
			daemon {
			  val gui = ExpGui(fig = fig, statusLabel = statusLabel)
			  exp.run(gui, fromJson = loadProp.value)
			  runLaterReturn {
				statusLabel.status.value = IDLE
				println("IDLE 1 ")
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
  stage.apply {
	/*isMaximized = true*/
	/*
	stage.x = 0.0
	stage.y = NEW_MAC_MENU_Y_ESTIMATE
	val screen = Screen.getPrimary()
	stage.height = screen.bounds.height - NEW_MAC_MENU_Y_ESTIMATE
	stage.width = screen.bounds.width*/
  }
  println("bottom of V1 main()")
}.start(
  shutdown = {
	/*as long as implcitExit is true, which it is, JavaFX should shutdown if there are 0 open windows and 0 pending runnables. I have confirmed that there are no open windows. So its entirely possible that chartfx or some other javafx library is doing some crazy recursive runLater or something so there are never 0 pending runnables. So I'm ok with forcing an exit. It's the best option available.*/
	println("shutting down normally")
	/*exitProcess(0)*/
	/*System.exit(0)*/
	/*welp... looks like we have a bigger problem on our hands*/
	Runtime.getRuntime().halt(0)
  }
)






