package matt.v1

import matt.gui.app.GuiApp
import matt.kbuild.DATA_FOLDER
import matt.kbuild.ismac
//import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.date.simplePrinting
import matt.kjlib.file.get
//import matt.reflect.ismac
import matt.reflect.onLinux
import matt.remote.host.Hosts
import matt.remote.runThisOnOM
import matt.remote.slurm.SRun
import matt.v1.gui.GuiMode
import matt.v1.gui.V1Gui
import matt.v1.gui.status.StatusLabel
import matt.v1.gui.status.StatusLabel.Status.IDLE
import matt.v1.gui.status.StatusLabel.Status.WORKING
import matt.v1.gui.vis.RosenbergVisualizer
import matt.v1.low.eulerBugChecker
import matt.v1.model.combined.ARI_BASE_CFG
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private const val REMOTE = false
private val REMOTE_AND_MAC = REMOTE && ismac

val visualizer by lazy { RosenbergVisualizer(ARI_BASE_CFG) }

val V1_DATA_FOLDER = DATA_FOLDER["kcomp"]["v1"]
val V1_USER_CFG_FILE = V1_DATA_FOLDER["usercfg.json"]



fun main(): Unit = GuiApp(screenIndex = 2) {

  simplePrinting = true

  eulerBugChecker()


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


  scene {
	root = V1Gui(startup = GuiMode.ROSENBERG, remoteStatus = remoteStatus)
  }
}.start(
  shutdown = {
	/*as long as implicitExit is true, which it is, JavaFX should shutdown if there are 0 open windows and 0 pending runnables. I have confirmed that there are no open windows. So its entirely possible that chartfx or some other javafx library is doing some crazy recursive runLater or something so there are never 0 pending runnables. So I'm ok with forcing an exit. It's the best option available.*/
	println("shutting down normally")
	/*exitProcess(0)*/
	/*System.exit(0)*/
	/*welp... looks like we have a bigger problem on our hands*/
	Runtime.getRuntime().halt(0)
  }
)






