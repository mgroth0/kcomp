package matt.v1.gui.fig

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.gsi.chart.XYChart
import de.gsi.chart.axes.spi.AbstractAxis
import de.gsi.chart.axes.spi.DefaultNumericAxis
import de.gsi.chart.renderer.LineStyle
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer
import de.gsi.dataset.spi.DoubleDataSet
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.exactWidthProperty
import matt.json.custom.toJsonWriter
import matt.json.prim.loadJson
import matt.kjlib.log.err
import matt.kjlib.stream.applyEach
import matt.klib.dmap.withStoringDefault
import matt.klib.log.warn
import matt.v1.comp.Fit.Gaussian
import matt.v1.lab.SeriesCfg
import matt.v1.lab.SeriesCfgs
import java.io.File
import kotlin.reflect.jvm.isAccessible

class Figure: Pane() {
  /*init{
	blue()
  }*/
  fun setup(
	chartTitle: String,
	xlabel: String,
	ylabel: String,
	xUnit: String,
	yUnit: String,
	autoY: Boolean,
	yMin: Double,
	yMax: Double,
	seriesCfgs: List<SeriesCfgs>,
	xMin: Double,
	xMax: Double,
	autoX: Boolean
  ) {


	/*what ive learned from a lot of trial and error and reading the library code is that forceRedraw always messes things up and I should avoid that completely. There seems to be no way to change the tick unit of an existing chart so i need to create a new one*/

	val xAxis = DefaultNumericAxis(
	  xlabel,
	  xMin,
	  xMax,
	  when {
		xMin == 0.0 && xMax == 100.0 -> 10.0
		else                         -> err("what tick unit to use for xMin=${xMin} and xMax = $xMax?")
	  }
	)
	val yAxis = DefaultNumericAxis(
	  ylabel,
	  yMin,
	  yMax,
	  when {
		yMin == 0.0 && yMax == 100.0 -> 10.0
		else                         -> err("what tick unit to use for yMin=${yMin} and yMax = $yMax?")
	  }
	)

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
	  title = "run an experiment"
	}

	println("chart.xAxis.tickUnit 1=${chart!!.xAxis.tickUnit}")
	println("chart.yAxis.tickUnit 1=${chart!!.yAxis.tickUnit}")

	/*chart.apply {
	  *//*layoutX = 0.0
	  layoutY = 0.0*//*

	}*/



	chart!!.title = chartTitle
	(chart!!.xAxis as DefaultNumericAxis).apply {
	  this.unit = xUnit
	  //	  name = xlabel
	  axisLabel.apply {
		text = xlabel
		fill = Color.WHITE
	  }
	  /*  if (!autoX) { *//*did i break above?*//*
		max = (xMax)
		min = xMin
	  }*/
	}
	(chart!!.yAxis as DefaultNumericAxis).apply {
	  this.unit = yUnit
	  //	  name = ylabel
	  axisLabel.apply {
		text = ylabel
		fill = Color.WHITE
	  }
	  /*  if (!autoY) { *//*did i break above?*//*
		max = (yMax)
		min = yMin
	  }*/

	}

	seriesCfgs.forEachIndexed { i, s ->
	  debugPrepName = s.label /*fixes bug a few lines below where setting name does nothing*/
	  @Suppress("UNUSED_VARIABLE") val make = series[i]
	  styleSeries(i = i, line = s.line, marker = s.markers)
	}

	var nextFitIndex = series.size
	seriesCfgs.forEach {
	  if ((it as? SeriesCfg)?.fit == Gaussian) {
		debugPrepName = "${it.label} (Gaussian fit)"
		@Suppress("UNUSED_VARIABLE") val make = series[nextFitIndex]
		styleSeries(i = nextFitIndex, line = true, marker = false)
		nextFitIndex++
	  }
	}

	//	DefaultNumericAxis()

	/*chart!!.xAxis.tickUnit = */

	/*chart!!.yAxis.tickUnit = */

	/*	chart!!.axes.forEach { a ->
		  (a as DefaultNumericAxis).apply {
			println("chart.xAxis.tickUnit=${chart!!.xAxis.tickUnit}")
			println("chart.yAxis.tickUnit=${chart!!.yAxis.tickUnit}")
			//		recomputeTickMarks()
			println("finished recomputeTickMarks")
		  }
		  *//*a.forceRedraw()*//*

	}*/
	/*lessForcefullRedraw()*/




	chart!!.legend.updateLegend(chart!!.datasets, chart!!.renderers, true)


	/*if (autoX) {
		autorangeXWith(xMin, xMax)
	  }*/

	//	lessForcefulRedraw()

	println("chart.xAxis.tickUnit 2=${chart!!.xAxis.tickUnit}")
	println("chart.yAxis.tickUnit 2=${chart!!.yAxis.tickUnit}")

  }

  fun lessForcefulRedraw() {
	if (System.currentTimeMillis() == 1L) {
	  chart!!.axes.forEach {
		it.forceRedraw()
		warn("this does recomputeTickMarks()")
	  }
	}
	chart!!.axes.forEach {
	  it as AbstractAxis


	  it.invalidateCaches() /*<- this is all about ticks...*/


	  it.recomputeTickMarks() /*<- dont do if i set my own ticks.*/


	  it.invalidate()


	  val layoutChildrenFun = it::class.members.first { it.name == "layoutChildren" }
	  layoutChildrenFun.isAccessible = true
	  layoutChildrenFun.call(it)


	  /*it.layoutChildren()*/
	}
  }


  fun dataToJson() = JsonArray().apply {
	chart!!.datasets.forEach { s ->
	  add(JsonArray().apply {
		(0 until s.dataCount).forEach { index ->
		  add(JsonObject().apply {
			addProperty("x", s[0, index])
			addProperty("y", s[1, index])
		  })
		}
	  })
	}
  }.toJsonWriter().toJsonString()

  fun loadJson(file: File) {
	file.loadJson(JsonArray::class).forEachIndexed { i, e ->
	  val s = series[i]
	  e.asJsonArray.forEachIndexed { ii, p ->
		val point = p.asJsonObject
		s.set(ii, point["x"].asDouble, point["y"].asDouble)
	  }
	}
  }

  var debugPrepName = "New Dataset"

  val seriesColors = listOf(
	"darkblue", "yellow", "lightgreen", "red", "pink", "white"
  ) //
  //  val seriesStyles = listOf(
  //	"markerColor=darkblue; markerType=circle;strokeColor=darkblue;strokeWidth=2;",
  //	"markerColor=yellow; markerType=circle;strokeColor=yellow;strokeWidth=2;"
  //  )

  fun styleSeries(i: Int, marker: Boolean, line: Boolean) {
	var s = ""
	s += if (marker) {
	  "markerColor: ${seriesColors[i]}; markerType: circle;"
	} else {
	  "markerColor: transparent;"
	}
	s += if (line) {
	  "strokeColor: ${seriesColors[i]}; strokeWidth: 2;"
	} else {    /*for now keep stroke color same as marker color for the legend*/    /*s += "strokeColor: ${seriesColors[i]}; strokeWidth: 2;"*/
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
	  val ds = DoubleDataSet(debugPrepName)
	  chart!!.datasets.add(ds)    /*  if (it <= seriesStyles.size-1) {
		  ds.setStyle(seriesStyles[it])
		  *//*ds.addDataStyle(seriesStyles[it])*//*
	  } */    //	  ds.style = seriesStyles[it]
	  chart!!.renderers.add(ErrorDataSetRenderer().apply {        /*	markerSize = 10.0
			isDrawMarker = true*/        /*polyLineStyle = LineStyle.NONE*/
		datasets.add(ds)
	  })
	  styleSeries(i = it, marker = false, line = true)    //	  chart.series("")
	}
	chart!!.datasets[it] as DoubleDataSet
  }


  fun autorangeY() {
	autorangeYWith(*series.values.toTypedArray())
  }

  fun autorangeX() = autorangeXWith(*series.values.toTypedArray())

  fun clear() {    /*found this method to be the least buggy and fastest, most responsive*/
	series.clear()
	children.remove(chart)
	/*newChart()*/
	/*NOW SETUP MUST BE CALLED*/
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

  private fun autorangeXWith(vararg series: DoubleDataSet) {
	val min = series.flatMap { it.xValues.toList() }.minOrNull()!!
	val max = series.flatMap { it.xValues.toList() }.maxOrNull()!!
	autorangeXWith(min, max)
  }

  fun autorangeXWith(min: Double, max: Double) {
	val diff = max - min
	val fivePercent = 0.05*diff
	chart!!.xAxis.min = min - fivePercent
	chart!!.xAxis.max = max + fivePercent
  }


  var chart: XYChart? = null
	private set
  /*
	init {
	  newChart()
	}*/
}
