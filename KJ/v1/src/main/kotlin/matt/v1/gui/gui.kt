package matt.v1.gui

import javafx.application.Platform.runLater
import javafx.collections.ObservableList
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart.Data
import javafx.scene.chart.XYChart.Series
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.text.Font
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.lib.onNonNullChange
import matt.hurricanefx.eye.prop.div
import matt.hurricanefx.eye.prop.times
import matt.hurricanefx.tornadofx.charts.linechart
import matt.hurricanefx.tornadofx.charts.series
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.control.slider
import matt.hurricanefx.tornadofx.item.combobox
import matt.kjlib.async.every
import matt.kjlib.date.sec
import matt.kjlib.itr.loopIterator
import matt.kjlib.jmath.sigFigs
import matt.v1.Status.IDLE
import matt.v1.Status.WORKING
import kotlin.reflect.KProperty

abstract class VisualizationCfg(val responsive: Boolean = false) {

  val props = mutableListOf<CfgProp<*>>()

  abstract fun update(): Any?

  abstract inner class CfgProp<T> {
	abstract var value: Any?
	lateinit var name: String
	operator fun provideDelegate(
	  thisRef: VisualizationCfg,
	  prop: KProperty<*>
	): CfgProp<T> {
	  props += this
	  name = prop.name
	  return this
	}

	operator fun getValue(
	  thisRef: VisualizationCfg,
	  property: KProperty<*>
	): T {
	  return value as T
	}

	operator fun setValue(
	  thisRef: VisualizationCfg,
	  property: KProperty<*>,
	  newValue: T
	) {
	  value = newValue
	}
  }

  sealed interface SliderProp

  inner class CfgObjProp<T>(vararg val values: T): CfgProp<T>() {
	override var value: Any? = values.first()
  }

  inner class CfgIntProp(val range: IntRange): CfgProp<Int>(), SliderProp {
	override var value: Any? = range.first.let {
	  val mid = (range.last - range.first) + range.first
	  var v = it
	  while (v < mid) {
		v += range.step
	  }
	  v
	}
  }

  inner class CfgDoubleProp(val range: Pair<Number, Number>): CfgProp<Double>(), SliderProp {
	override var value: Any? = ((range.second.toDouble() + range.first.toDouble())/2.0) + range.first.toDouble()
  }


  val cfgPane by lazy {
	FlowPane().apply {
	  val fp = this
	  hgap = 10.0
	  vgap = 10.0
	  props.forEach { p ->
		when (p) {
		  is CfgObjProp<*> -> combobox(values = p.values.toList()) {
			value = p.value
			maxWidthProperty().bind(fp.widthProperty())
			promptText = p.name + "?"
			valueProperty().onChange {
			  p.value = it
			  update()
			}
		  }
		  is SliderProp    -> label(p.name) {
			when (p) {
			  is CfgIntProp    -> slider(min = p.range.first, max = p.range.last, value = (p.value as Int).toDouble())
			  is CfgDoubleProp -> slider(min = p.range.first, max = p.range.second, value = p.value as Double)
			}.apply {
			  maxWidthProperty().bind(fp.widthProperty())
			  isSnapToTicks = p is CfgIntProp
			  isShowTickMarks = p is CfgIntProp
			  isShowTickLabels = p is CfgIntProp
			  majorTickUnit = 1.0
			  minorTickCount = 0
			  fun updateText() {
				text = when (p) {
				  is CfgIntProp    -> "${p.name}:$value"
				  is CfgDoubleProp -> "${p.name}:${value.sigFigs(3)}"
				}
			  }
			  updateText()
			  when (responsive) {
				true  -> {
				  valueProperty().onChange {
					runLater {
					  p.value = it
					  updateText()
					  update()
					}
				  }
				}
				false -> {
				  valueChangingProperty().onChange {
					if (!it) {
					  /*I think this runLater is necessary or else the wrong value comes through when snap to ticks is true*/
					  runLater {
						p.value = value
						updateText()
						update()
					  }
					}
				  }
				}
			  }
			}
		  }
		}
	  }
	}
  }
}

class Figure: Pane() {
  lateinit var series1: Series<Number, Number>
	private set
  lateinit var series2: Series<Number, Number>
	private set

  fun autorangeY() = autorangeYWith(series1.data, series2.data)
  fun clear() {

	/*turns out JavaFX charts are super buggy and I have to do it this way*/

	/*series1 =

	*//*series1.data.clear()*//*
	series1.data.iterateM { remove() }
	series2.data.iterateM { remove() }
	*//*series2.data.clear()*//*



	series1.name = ""
	series2.name = ""
	chart.xAxis.label = ""
	chart.yAxis.label = ""
	chart.title = ""*/
	children.remove(chart)
	newChart()
	/*I like it. Much cleaner, changes fast, actually feels more responsive.*/
  }


  fun autorangeYWith(vararg series: ObservableList<Data<Number, Number>>) {
	val min = series.flatMap { it }.minOfOrNull { it.yValue.toDouble() }!!
	val max = series.flatMap { it }.maxOfOrNull { it.yValue.toDouble() }!!
	val diff = max - min
	val tenPercent = 0.1*diff
	(chart.yAxis as NumberAxis).lowerBound = min - tenPercent
	(chart.yAxis as NumberAxis).upperBound = max + tenPercent
  }

  private fun newChart() {
	chart = linechart(
	  "run an experiment",
	  NumberAxis().apply {
		isAutoRanging = false
		isTickMarkVisible = true
		isTickLabelsVisible = true
		minorTickCount = 5
		tickUnitProperty().bind(upperBoundProperty().div(5))
	  },
	  NumberAxis().apply {
		isAutoRanging = false
		isTickMarkVisible = true
		isTickLabelsVisible = true
		minorTickCount = 5
		tickUnitProperty().bind(upperBoundProperty().div(5))
		/*tickLabelFormatter = BYTE_SIZE_FORMATTER*/
	  }
	) {
	  series1 = series("")
	  series2 = series("")
	  createSymbols = false
	  if (scene != null) {
		exactWidthProperty().bind(scene.widthProperty()*0.9)
	  }
	  sceneProperty().onNonNullChange {
		exactWidthProperty().bind(it.widthProperty()*0.9)
	  }

	  this.animated = false
	  layoutX = 0.0
	  layoutY = 0.0
	}
  }

  lateinit var chart: LineChart<Number, Number> private set

  init {
	newChart()
  }
}

class StatusLabel: HBox()/*so label is steady*/ {
  var status = Prop(IDLE)
  var statusExtra = ""

  init {
	prefWidth = 100.0 /*so label is steady*/
	layoutX = 0.0
	layoutY = 0.0
	val statusLabel = label("") {
	  font = Font.font("Monospaced")
	}


	val dotItr = (1..3).toList().loopIterator()
	every(0.05.sec, ownTimer = true) {
	  when (status.value!!) {
		IDLE    -> runLaterReturn { statusLabel.text = "" }
		WORKING -> runLaterReturn {
		  statusLabel.text =
			  (statusExtra.takeIf { it.isNotBlank() }
			   ?: "working") + (0 until dotItr.next()).joinToString(separator = "") { "." }
		}
	  }
	}
  }
}
