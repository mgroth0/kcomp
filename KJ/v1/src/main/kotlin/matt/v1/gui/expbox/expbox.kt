package matt.v1.gui.expbox

import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventTarget
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.Button
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import matt.auto.subl
import matt.fxlib.console.customConsole
import matt.fxlib.console.interceptConsole
import matt.gui.core.context.mcontextmenu
import matt.gui.lang.withAction
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.addr
import matt.hurricanefx.dragsSnapshot
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.doubleBinding
import matt.hurricanefx.op
import matt.hurricanefx.tornadofx.control.button
import matt.hurricanefx.tornadofx.control.checkbox
import matt.hurricanefx.tornadofx.control.text
import matt.hurricanefx.tornadofx.layout.hbox
import matt.hurricanefx.tornadofx.layout.vbox
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.disableWhen
import matt.hurricanefx.tornadofx.nodes.vgrow
import matt.hurricanefx.tornadofx.tab.staticTab
import matt.hurricanefx.tornadofx.tab.tabpane
import matt.kjlib.async.daemon
import matt.kjlib.map.lazyMap
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.cap
import matt.v1.exps.experiments
import matt.v1.exps.expmodels.ExpCategory
import matt.v1.gui.fig.Figure
import matt.v1.gui.status.StatusLabel
import matt.v1.gui.status.StatusLabel.Status.IDLE
import matt.v1.gui.status.StatusLabel.Status.WORKING
import matt.v1.lab.Experiment.Companion.EXPS_DATA_FOLDER
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

private val loadProp = BProp(true)

fun EventTarget.figBox(statusLabel: StatusLabel, opp: HBox.()->Unit) = hbox {
  alignment = CENTER
  vbox {
	checkbox("load", loadProp)
	button("open data folder") withAction EXPS_DATA_FOLDER::subl
  }
  mcontextmenu {
	checkitem("load", loadProp)
	"toggle square fig" toggles squareFigProp
  }
  val fig = addr(Figure()) {
	vgrow = ALWAYS
	exactWidthProperty().bind(squareFigProp.doubleBinding(heightProperty()) {
	  if (it!!) height else Double.MAX_VALUE
	})
	dragsSnapshot()
  }
  val rightBox = vbox {
	text("fig controls")
	//	blue()
  }
  val expConsole = rightBox.customConsole(takesInput = false) {
	//	red()
  }
  val (writer, reader) = expConsole.custom()
  val sysConsole = rightBox.interceptConsole {
	interceptStdOutErr()
	//	yellow()
  }
  val allButtons = mutableListOf<Button>()
  experiments().forEach { exp ->
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
		  fig.clear()
		  fig.setup(
			chartTitle = exp.title,
			xAxisConfig = exp.xAxisConfig,
			yAxisConfig = exp.yAxisConfig,
			seriesCfgs = exp.series,
		  )
		  daemon {
			val gui = ExpGui(fig = fig, statusLabel = statusLabel, console = writer)
			exp.run(gui, fromJson = loadProp.value)
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
  opp()
}