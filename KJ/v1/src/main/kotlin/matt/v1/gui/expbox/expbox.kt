package matt.v1.gui.expbox

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventTarget
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.Button
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import matt.async.daemon
import matt.auto.openInFinder
import matt.auto.subl
import matt.caching.compcache.COMP_CACHE_FOLDER
import matt.caching.compcache.ComputeCache
import matt.fxlib.console.customConsole
import matt.fxlib.console.interceptConsole
import matt.gui.core.context.mcontextmenu
import matt.gui.lang.withAction
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.addr
import matt.hurricanefx.dragsSnapshot
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.doubleBinding
import matt.hurricanefx.op
import matt.hurricanefx.tornadofx.control.button
import matt.hurricanefx.tornadofx.control.checkbox
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.tornadofx.layout.hbox
import matt.hurricanefx.tornadofx.layout.vbox
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.disableWhen
import matt.hurricanefx.tornadofx.nodes.vgrow
import matt.hurricanefx.tornadofx.tab.staticTab
import matt.hurricanefx.tornadofx.tab.tabpane
import matt.kjlib.map.lazyMap
import matt.klib.lang.cap
import matt.klib.str.addSpacesUntilLengthIs
import matt.v1.cfg.user.UserConfig
import matt.v1.exps.experiments
import matt.v1.exps.expmodels.ExpCategory
import matt.v1.gui.fig.Figure
import matt.v1.gui.status.StatusLabel
import matt.v1.gui.status.StatusLabel.Status.IDLE
import matt.v1.gui.status.StatusLabel.Status.WORKING
import matt.v1.lab.Experiment.Companion.EXPS_DATA_FOLDER
import matt.v1.low.Norm
import matt.v1.scaling.PerformanceMode
import java.io.PrintWriter

val expCategoryPanes = lazyMap<ExpCategory, FlowPane> { FlowPane() }
val ExpCategory.pane get() = expCategoryPanes[this]!!

fun EventTarget.expBox(opp: VBox.()->Unit) = vbox {
  alignment = TOP_CENTER
  this.vgrow = ALWAYS
  tabpane {
	this.vgrow = ALWAYS
	ExpCategory.values().forEach {
	  staticTab(it.name.lowercase().cap(), it.pane)
	}
	exactHeight = 120.0
  }
  val statusLabel = StatusLabel()
  figBox(statusLabel) {
	vgrow = ALWAYS
  }
  add(statusLabel)
  opp()
}


class ExpGui(
  val fig: Figure,
  val statusLabel: StatusLabel,
  val console: PrintWriter
)

val squareFigProp = SimpleBooleanProperty(true)
val somethingRunningProp = SimpleBooleanProperty(false)

fun EventTarget.figBox(statusLabel: StatusLabel, opp: HBox.()->Unit) = hbox {
  alignment = CENTER
  val exps = experiments()
  vbox {
	checkbox("save", UserConfig.saveExpsProp) {
	  disableWhen { somethingRunningProp }
	}
	checkbox("load", UserConfig.loadExpsProp) {
	  disableWhen { somethingRunningProp }
	}
	choicebox<PerformanceMode>(UserConfig.scaleProp, PerformanceMode.values().toList()) {
	  disableWhen { somethingRunningProp }
	}
	button("open data folder") withAction EXPS_DATA_FOLDER::subl
	button("open cache folder") withAction COMP_CACHE_FOLDER::openInFinder
	button("save norm cache") withAction {
	  ComputeCache.saveCache<Norm,Double>()
	}
  }
  mcontextmenu {
	checkitem("load", UserConfig.loadExpsProp as BooleanProperty)
	"toggle square fig" toggles squareFigProp
  }
  val fig = addr(Figure()) {
	vgrow = ALWAYS
	exactWidthProperty().bind(squareFigProp.doubleBinding(heightProperty()) {
	  if (it!!) height else Double.MAX_VALUE
	})
	dragsSnapshot()
  }
  var figControlBox: VBox? = null
  val rightBox = vbox()
  val expConsole = rightBox.customConsole(takesInput = false) {
	//	red()
  }
  val (writer, reader) = expConsole.custom()
  val sysConsole = rightBox.interceptConsole {
	interceptStdOutErr()
	//	yellow()
  }
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
		  statusLabel.counters.clear()
		  statusLabel.status.value = WORKING
		  rightBox.children.remove(figControlBox)
		  fig.clear()
		  fig.setup(
			chartTitle = exp.title,
			xAxisConfig = exp.xAxisConfig,
			yAxisConfig = exp.yAxisConfig,
			seriesCfgs = exp.series,
		  )
		  rightBox.children.add(0, fig.controlBox())
		  daemon {
			val gui = ExpGui(fig = fig, statusLabel = statusLabel, console = writer)
			exp.run(gui, fromJson = UserConfig.loadExps)
			runLaterReturn {
			  statusLabel.status.value = IDLE
			}
		  }
		}
	  }

	  exp.runningProp.onChange {
		when (it) {
		  true  -> {
			somethingRunningProp.value = true
			text = "stop"
			op = exp::stop
		  }
		  false -> {
			cfgStartButton()
			somethingRunningProp.value = exps.any { it.runningProp.value }
		  }
		}
	  }
	  cfgStartButton()
	}.also {
	  it.disableWhen { somethingRunningProp }
	}
  }
  opp()
}