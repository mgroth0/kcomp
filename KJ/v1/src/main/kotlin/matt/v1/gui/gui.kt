package matt.v1.gui

import de.gsi.chart.XYChart
import de.gsi.chart.axes.spi.DefaultNumericAxis
import de.gsi.chart.renderer.LineStyle
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer
import de.gsi.dataset.spi.DoubleDataSet
import javafx.application.Platform.runLater
import javafx.event.EventTarget
import javafx.scene.canvas.Canvas
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.text.Font
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.exactWidth
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.lib.onNonNullChange
import matt.hurricanefx.eye.prop.times
import matt.hurricanefx.tornadofx.control.checkbox
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.control.slider
import matt.hurricanefx.tornadofx.fx.opcr
import matt.hurricanefx.tornadofx.item.combobox
import matt.kjlib.async.every
import matt.kjlib.date.sec
import matt.kjlib.itr.loopIterator
import matt.kjlib.jmath.sigFigs
import matt.klib.dmap.withStoringDefault
import matt.v1.Status.IDLE
import matt.v1.Status.WORKING
import kotlin.reflect.KProperty

//import de.gsi.chart.XYChart
//import de.gsi.chart.axes.spi.DefaultNumericAxis
//import de.gsi.dataset.spi.DoubleDataSet

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

  inner class CfgBoolProp(default: Boolean): CfgProp<Boolean>() {
	override var value: Any? = default
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
			/*maxWidthProperty().bind(fp.widthProperty())*/
			exactWidthProperty().bind(fp.widthProperty()*.4)
			promptText = p.name + "?"
			valueProperty().onChange {
			  p.value = it
			  update()
			}
		  }
		  is CfgBoolProp   -> checkbox(p.name) {
			isSelected = p.value as Boolean
			/*maxWidthProperty().bind(fp.widthProperty())*/
			exactWidthProperty().bind(fp.widthProperty()*.4)
			//			promptText = p.name + "?"
			selectedProperty().onChange {
			  p.value = it
			  update()
			}
		  }
		  is SliderProp    -> label(p.name) {
			exactWidthProperty().bind(fp.widthProperty()*.4)
			when (p) {
			  is CfgIntProp    -> slider(min = p.range.first, max = p.range.last, value = (p.value as Int).toDouble())
			  is CfgDoubleProp -> slider(min = p.range.first, max = p.range.second, value = p.value as Double)
			}.apply {

			  /*maxWidthProperty().bind(fp.widthProperty())*/
			  isSnapToTicks = p is CfgIntProp
			  isShowTickMarks = p is CfgIntProp
			  isShowTickLabels = p is CfgIntProp
			  majorTickUnit = 1.0
			  minorTickCount = 0
			  fun updateText() {
				text = when (p) {
				  is CfgIntProp    -> "${p.name}:${value}"
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

class Figure(
  //  var line: Boolean = true
): Pane() {

  var debugPrepName = "New Dataset"

  val seriesColors = listOf(
	"darkblue",
	"yellow",
	"lightgreen",
	"red",
	"pink",
	"white"
  )
  //
  //  val seriesStyles = listOf(
  //	"markerColor=darkblue; markerType=circle;strokeColor=darkblue;strokeWidth=2;",
  //	"markerColor=yellow; markerType=circle;strokeColor=yellow;strokeWidth=2;"
  //  )

  fun styleSeries(i: Int, marker: Boolean, line: Boolean) {
	var s = ""
	if (marker) {
	  s += "markerColor: ${seriesColors[i]}; markerType: circle;"
	} else {
	  s += "markerColor: transparent;"
	}
	s += if (line) {
	  "strokeColor: ${seriesColors[i]}; strokeWidth: 2;"
	} else {
	  /*for now keep stroke color same as marker color for the legend*/
	  /*s += "strokeColor: ${seriesColors[i]}; strokeWidth: 2;"*/
	  "strokeColor: transparent; strokeWidth: 0;"
	}
	series[i].style = s
	(chart.renderers[i] as ErrorDataSetRenderer).apply {
	  this.isDrawMarker = marker
	  this.markerSize = if (marker) 10.0 else 0.0
	  this.polyLineStyle = if (line) LineStyle.NORMAL else LineStyle.NONE
	}
  }

  val series = mutableMapOf<Int, DoubleDataSet>().withStoringDefault {
	while (it + 1 > chart.datasets.size) {
	  val ds = DoubleDataSet(debugPrepName)
	  chart.datasets.add(ds)
	  /*  if (it <= seriesStyles.size-1) {
		  ds.setStyle(seriesStyles[it])
		  *//*ds.addDataStyle(seriesStyles[it])*//*
	  } */
	  //	  ds.style = seriesStyles[it]
	  chart.renderers.add(ErrorDataSetRenderer().apply {
		/*	markerSize = 10.0
			isDrawMarker = true*/
		/*polyLineStyle = LineStyle.NONE*/
		datasets.add(ds)
	  })
	  styleSeries(i = it, marker = false, line = true)
	  //	  chart.series("")
	}
	chart.datasets[it] as DoubleDataSet
  }

  /*  fun setSeriesShowLine(i: Int, b: Boolean) {
	  *//*if (b == false) {
	  *//**//*if (b) NORMAL*//**//* *//**//*removes color, and in a buggy way where legend still has color*//**//*
	  (chart.renderers[i] as ErrorDataSetRenderer).polyLineStyle = LineStyle.NONE
	}*//*
	(chart.renderers[i] as ErrorDataSetRenderer).polyLineStyle = LineStyle.NONE

	*//*chart.lookup(".default-color${i}.chart-series-line")?.style = if (b) "-fxstroke: transparent" else "";*//*
  }*/

  /*  fun setSeriesShowMarkers(i: Int, b: Boolean) {
	  *//*if (b==false) {
	  (chart.renderers[i] as ErrorDataSetRenderer).isDrawMarker = b
	}
*//*
	(chart.renderers[i] as ErrorDataSetRenderer).isDrawMarker = b
	(chart.renderers[i] as ErrorDataSetRenderer).markerSize = 0.0
	*//*chart.lookup(".default-color${i}.chart-line-symbol")?.style = if (b) "-fxstroke: transparent" else ""*//*
  }*/

  fun autorangeY() = autorangeYWith(*series.values.toTypedArray())
  fun autorangeX() = autorangeXWith(*series.values.toTypedArray())
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
	series.clear()
	children.remove(chart)
	newChart()
	/*I like it. Much cleaner, changes fast, actually feels more responsive.*/
  }


  private fun autorangeYWith(vararg series: DoubleDataSet) {


	val min = series.flatMap { it.yValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.yValues.toList() }.maxOrNull()!!
	val diff = max - min
	val tenPercent = 0.1*diff
	chart.yAxis.min = min - tenPercent
	chart.yAxis.max = max + tenPercent
  }

  private fun autorangeXWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.xValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.xValues.toList() }.maxOrNull()!!
	autorangeXWith(min, max)
  }

  fun autorangeXWith(min: Double, max: Double) {
	val diff = max - min
	val fivePercent = 0.05*diff
	chart.xAxis.min = min - fivePercent
	chart.xAxis.max = max + fivePercent
  }

  private fun newChart() {
	val axis1 = DefaultNumericAxis().apply {
	  isAutoRanging = false
	  isTickMarkVisible = true
	  isTickLabelsVisible = true
	  minorTickCount = 5
	  /*maxMajorTickLabelCount = 5
	  tick
	  tickUnitProperty().bind(maxProperty().minus(minProperty()).div(5))*/

	}
	val axis2 = DefaultNumericAxis().apply {
	  isAutoRanging = false
	  isTickMarkVisible = true
	  isTickLabelsVisible = true
	  minorTickCount = 5
	  /*tickUnitProperty().bind(maxProperty().minus(minProperty()).div(5))
	  *//*tickLabelFormatter = BYTE_SIZE_FORMATTER*/
	}
	chart =


		/*if (line)*/ XYChart(axis1, axis2).apply {
	  this@Figure.children.add(this)
	  title = "run an experiment"
	} /*else scatterchart(
		  "run an experiment",
		  axis1,
		  axis2
		) {}*/
	chart.apply {
	  //	  (this as? LineChart)?.createSymbols = false
	  if (scene != null) {
		exactWidthProperty().bind(scene.widthProperty()*0.9)
	  }
	  sceneProperty().onNonNullChange {
		exactWidthProperty().bind(it.widthProperty()*0.9)
	  }
	  //		this.ani
	  //	  this.animated = false
	  layoutX = 0.0
	  layoutY = 0.0
	  exactHeightProperty().bind(this@Figure.heightProperty())
	  exactWidthProperty().bind(this@Figure.widthProperty())
	}
  }

  lateinit var chart: XYChart private set

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


