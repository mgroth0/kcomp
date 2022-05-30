package matt.v1.gui.fig

import de.gsi.chart.XYChart
import de.gsi.chart.renderer.LineStyle
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer
import de.gsi.dataset.spi.DoubleDataSet
import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import matt.gui.clip.copyToClipboard
import matt.gui.core.context.mcontextmenu
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.control.textfield
import matt.json.custom.double
import matt.json.prim.loadJson
import matt.kjlib.jmath.ranges.step
import matt.klib.dmap.withStoringDefault
import matt.stream.applyEach
import matt.v1.comp.Fit.Gaussian
import matt.v1.figmodels.AxisConfig
import matt.v1.figmodels.SeriesCfg
import matt.v1.figmodels.SeriesCfgs
import matt.v1.gui.fig.axes.ControlledNumericAxis
import java.io.File

fun calcControlledTickValues(rangeMin: Number?, rangeMax: Number?) = when {
  rangeMin !is Number || rangeMax !is Number -> null
  rangeMin == 0.0 && rangeMax == 100.0       -> (0.0..100.0 step 10.0).toMutableList()
  rangeMin in 0 until 5 && rangeMax in 5..10 -> (0.0..10.0 step 1.0).toMutableList()
  else                                       -> null
}

class Figure: Pane() {

  init {
	mcontextmenu {
	  "copy data as json" does {
		dataToJson().copyToClipboard()
	  }
	}
  }

  companion object {
	private val DEFAULT_AXIS_CONFIG = AxisConfig(
	  min = null,
	  max = null,
	  unit = null,
	  label = "insert axis label here"
	)
  }


  fun setup(
	chartTitle: String = "insert title here",
	xAxisConfig: AxisConfig = DEFAULT_AXIS_CONFIG,
	yAxisConfig: AxisConfig = DEFAULT_AXIS_CONFIG,
	seriesCfgs: List<SeriesCfgs> = listOf(),
  ) {
	val xAxis = ControlledNumericAxis(xAxisConfig).apply {
	  println("calcing for ${xAxisConfig.min},${xAxisConfig.max}")
	  controlledTickValues = calcControlledTickValues(xAxisConfig.min, xAxisConfig.max)
	}
	val yAxis = ControlledNumericAxis(yAxisConfig).apply {
	  controlledTickValues = calcControlledTickValues(yAxisConfig.min, yAxisConfig.max)
	}
	arrayOf(xAxis, yAxis).applyEach {
	  isAutoRanging = false
	  isTickMarkVisible = true
	  isTickLabelsVisible = true
	  isMinorTickVisible = false
	}
	chart = XYChart(xAxis, yAxis).apply {
	  exactHeightProperty().bind(this@Figure.heightProperty())
	  exactWidthProperty().bind(this@Figure.widthProperty())
	  this@Figure.children.add(this)
	  title = chartTitle
	  triggerDistance = 0.0
	}
	seriesCfgs.forEachIndexed { i, s ->    /* debugPrepName = s.label*/ /*fixes bug a few lines below where setting name does nothing*/
	  @Suppress("UNUSED_VARIABLE") val make = series[i]
	  styleSeries(i = i, line = s.line, marker = s.markers)
	  series[i].name = s.label
	}
	var nextFitIndex = series.size
	seriesCfgs.forEach {
	  if ((it as? SeriesCfg)?.fit == Gaussian) {        /*debugPrepName = "${it.label} (Gaussian fit)"*/
		@Suppress("UNUSED_VARIABLE") val make = series[nextFitIndex]
		styleSeries(i = nextFitIndex, line = true, marker = false)
		series[nextFitIndex].name = "${it.label} (Gaussian fit)"
		nextFitIndex++
	  }
	}
	chart!!.legend.updateLegend(
	  chart!!.datasets, chart!!.renderers, true
	)
  }


  /*actually this shouldnt be deprecated since its useful for exports*/
  fun dataToJson() =
	buildJsonArray {
	  chart!!.datasets.forEach { s ->
		add(
		  buildJsonArray {
			(0 until s.dataCount).forEach { index ->
			  add(
				buildJsonObject {
				  put("x", JsonPrimitive(s[0, index]))
				  put("y", JsonPrimitive(s[1, index]))
				}
			  )
			}
		  }
		)
	  }
	}.toString()


  @Deprecated(
	"use GuiUpdate.fromJson() instead, or if I really want to do this I can reimplement it later but currently it probably wont work right"
  )
  fun loadJson(file: File) {
	file.loadJson(JsonArray::class).forEachIndexed { i, e ->
	  val s = series[i]
	  e.jsonArray.forEachIndexed { ii, p ->
		val point = p.jsonObject
		s.set(ii, point["x"]!!.double, point["y"]!!.double)
	  }
	}
  }

  //  var debugPrepName = "New Dataset"

  val seriesColors = listOf(
	"darkblue", "yellow", "lightgreen", "red", "pink", "white"
  )

  fun styleSeries(i: Int, marker: Boolean, line: Boolean) {
	val color = seriesColors[i]
	var s = ""
	s += if (marker) {
	  "markerColor: $color; markerType: circle;"
	} else {
	  "markerColor: transparent;"
	}
	s += if (line) {
	  "strokeColor: $color; fillColor: $color; strokeWidth: 2;"
	} else {    /*for now keep stroke color same as marker color for the legend*/
	  "strokeColor: transparent; strokeWidth: 0;"
	}
	series[i].style = s
	(chart!!.renderers[i] as ErrorDataSetRenderer).apply {
	  this.isDrawMarker = marker
	  this.markerSize = if (marker) 10.0 else 0.0
	  this.polyLineStyle = if (line) LineStyle.NORMAL else LineStyle.NONE
	}
  }

  val series = mutableMapOf<Int, DoubleDataSet>().withStoringDefault {
	while (it + 1 > chart!!.datasets.size) {
	  val ds = DoubleDataSet("new dataset" /*debugPrepName*/)
	  chart!!.datasets.add(ds)
	  chart!!.renderers.add(ErrorDataSetRenderer().apply {
		datasets.add(ds)
	  })
	  styleSeries(i = it, marker = false, line = true)    //	  chart.series("")
	}
	chart!!.datasets[it] as DoubleDataSet
  }


  fun autorangeY() {
	autorangeYWith(*series.values.toTypedArray())
  }

  fun autorangeYmin() {
	autorangeYMinWith(*series.values.toTypedArray())
  }

  fun autorangeYmax() {
	autorangeYMaxWith(*series.values.toTypedArray())
  }

  fun autorangeX() = autorangeXWith(*series.values.toTypedArray())

  fun autorangeXmin() {
	autorangeXMinWith(*series.values.toTypedArray())
  }

  fun autorangeXmax() {
	autorangeXMaxWith(*series.values.toTypedArray())
  }

  fun clear() {    /*found this method to be the least buggy and fastest, most responsive*/
	series.clear()
	children.remove(chart)    /*newChart()*/    /*NOW SETUP MUST BE CALLED*/
	chart = null
  }


  private fun autorangeYWith(vararg series: DoubleDataSet) {

	val min = series.flatMap { it.yValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.yValues.toList() }.maxOrNull()!!
	val diff = max - min
	val tenPercent = 0.1*diff
	chart!!.yAxis.min = min - tenPercent
	chart!!.yAxis.max = max + tenPercent
  }

  private fun autorangeYMinWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.yValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.yValues.toList() }.maxOrNull()!!
	val diff = max - min
	val tenPercent = 0.1*diff
	chart!!.yAxis.min = min - tenPercent
  }

  private fun autorangeYMaxWith(vararg series: DoubleDataSet) {

	val min = series.flatMap { it.yValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.yValues.toList() }.maxOrNull()!!
	val diff = max - min
	val tenPercent = 0.1*diff
	chart!!.yAxis.max = max + tenPercent
  }

  private fun autorangeXWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.xValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.xValues.toList() }.maxOrNull()!!
	autorangeXWith(min, max)
  }

  private fun autorangeXMinWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.xValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.xValues.toList() }.maxOrNull()!!
	autorangeXMinWith(min, max)
  }

  private fun autorangeXMaxWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.xValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.xValues.toList() }.maxOrNull()!!
	autorangeXMaxWith(min, max)
  }

  fun autorangeXWith(min: Double, max: Double) {
	val diff = max - min
	val fivePercent = 0.05*diff
	chart!!.xAxis.min = min - fivePercent
	chart!!.xAxis.max = max + fivePercent
  }

  fun autorangeXMinWith(min: Double, max: Double) {
	val diff = max - min
	val fivePercent = 0.05*diff
	chart!!.xAxis.min = min - fivePercent
  }

  fun autorangeXMaxWith(min: Double, max: Double) {
	val diff = max - min
	val fivePercent = 0.05*diff
	chart!!.xAxis.max = max + fivePercent
  }

  fun controlBox() = VBox().apply {
	label(text = "xMin", graphic = textfield(chart!!.xAxis.minProperty())) {
	  contentDisplay = RIGHT
	}
  }

  var chart: XYChart? = null
	private set

  init {
	setup() /*so the figure is drawn and takes up space even before any data is added, which is expected behavior in matlab and similar platforms*/
  }
}
