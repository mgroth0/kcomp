package matt.v1.gui.fig

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.gsi.chart.XYChart
import de.gsi.chart.axes.spi.DefaultNumericAxis
import de.gsi.chart.renderer.LineStyle
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer
import de.gsi.dataset.spi.DoubleDataSet
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.exactWidthProperty
import matt.json.custom.toJsonWriter
import matt.json.prim.loadJson
import matt.kjlib.async.every
import matt.kjlib.async.with
import matt.kjlib.date.sec
import matt.klib.dmap.withStoringDefault
import matt.v1.comp.Fit.Gaussian
import matt.v1.compcache.Point
import matt.v1.gui.fig.SeriesUpdateType.APPEND
import matt.v1.gui.fig.SeriesUpdateType.REPLACE
import matt.v1.lab.Experiment.RunStage
import matt.v1.lab.Experiment.RunStage.FIG_COMPLETE
import matt.v1.lab.Experiment.RunStage.WAITING_FOR_FIG
import matt.v1.lab.SeriesCfg
import matt.v1.lab.SeriesCfgs
import java.io.File
import java.util.concurrent.Semaphore


interface FigUpdate {
  fun toFigUpdate(): FigureUpdate

}

fun replaceNextSeries(points: List<Point>) = SeriesUpdate(
  type = REPLACE,
  seriesIndex = null,
  points = points
)

class FigureUpdate(
  val updates: List<SeriesUpdate>
): FigUpdate {
  override fun toFigUpdate() = this
  override fun toString(): String {
	var s = "FigureUpdate:"
	updates.forEach { s += "\n\t${it}" }
	return s
  }
}

enum class SeriesUpdateType {
  REPLACE, APPEND
}

infix fun Int.replaceWith(points: List<Point>) = SeriesUpdate(
  type = REPLACE,
  seriesIndex = this,
  points = points
)

data class SeriesUpdate(
  val type: SeriesUpdateType,
  val seriesIndex: Int?,
  val points: List<Point>
): FigUpdate {
  constructor(type: SeriesUpdateType, seriesIndex: Int, x: Double, y: Double): this(
	type = type, seriesIndex = seriesIndex, p = Point(x = x, y = y)
  )

  constructor(type: SeriesUpdateType, seriesIndex: Int, p: Point): this(
	type = type, seriesIndex = seriesIndex, points = listOf(p)
  )

  override fun toFigUpdate() = FigureUpdate(listOf(this))
  override fun toString(): String {
	var s = "SeriesUpdate type=${type}, seriesIndex=${seriesIndex}, points:"
	points.forEach {
	  s += "\n\t\t${it}"
	}
	return s
  }

}

class FigureUpdater(
  val fig: Figure,
  autoY: Boolean,
  autoX: Boolean,
  getRunStage: ()->RunStage,
  getStopped: ()->Boolean,
  getRunning: ()->Boolean,
  setRunStage: (RunStage)->Unit
) {
  companion object {
	const val FIG_REFRESH_PERIOD_SECS = 0.1
	val figSem = Semaphore(1)
  }

  val seriesPoints = mutableMapOf<Int, MutableList<Point>>(0 to mutableListOf())
  val figNextPointsI = mutableMapOf(0 to 0)
  private var lastLegendUpdate = 0
  val timerTask = every(FIG_REFRESH_PERIOD_SECS.sec) {
	figSem.with {
	  if (seriesPoints.all {
		  if (it.key !in figNextPointsI) figNextPointsI[it.key] = 0
		  it.value.size == figNextPointsI[it.key]
		} && getRunStage() in listOf(WAITING_FOR_FIG, FIG_COMPLETE)) {
		if (getStopped()) this.cancel()
		if (!getRunning()) this.cancel()
	  }
	}
	runLaterReturn {
	  figSem.with {
		seriesPoints.forEach { (i, list) ->
		  if (figNextPointsI[i] == 0) {
			fig.series[i].set(
			  list.subList(figNextPointsI[i]!!, list.size).map { it.x }.toDoubleArray(),
			  list.subList(figNextPointsI[i]!!, list.size).map { it.y }.toDoubleArray()
			)
		  } else if (figNextPointsI[i]!! < seriesPoints[i]!!.size) {
			fig.series[i].add(
			  list.subList(figNextPointsI[i]!!, list.size).map { it.x }.toDoubleArray(),
			  list.subList(figNextPointsI[i]!!, list.size).map { it.y }.toDoubleArray()
			)
		  }
		  figNextPointsI[i] = seriesPoints[i]!!.size
		  /*  if (getRunStage() == WAITING_FOR_FIG) {
			  setRunStage(FIG_COMPLETE)
			}*/
		}
		if (autoY && figNextPointsI.values.any { it != 0 }) fig.autorangeY()
		if (autoX && figNextPointsI.values.any { it != 0 }) fig.autorangeX()
		fig.chart.axes.forEach { a -> a.forceRedraw() } /*sadly this seems necessary do to internal bugs of the library*/
		if (lastLegendUpdate < figNextPointsI.size) {
		  fig.chart.legend.updateLegend(fig.chart.datasets, fig.chart.renderers, true)
		  lastLegendUpdate = figNextPointsI.size
		}
		if (getRunStage() == WAITING_FOR_FIG) {
		  setRunStage(FIG_COMPLETE)
		}
	  }
	}
  }

  fun sendPoints(seriesIndex: Int, points: List<Point>) {
	figSem.with {
	  seriesPoints[seriesIndex] = points.toMutableList()
	  figNextPointsI[seriesIndex] = 0
	}
  }

  var nextSeriesUpdateIndex = 0
  fun update(figureUpdate: FigureUpdate) {
	/*println("GOT FIGURE UPDATE: ${figureUpdate}")*/
	figSem.with {
	  figureUpdate.updates.forEach {
		val index = it.seriesIndex ?: run {
		  nextSeriesUpdateIndex.also {
			nextSeriesUpdateIndex++
			if (nextSeriesUpdateIndex == fig.series.size) {
			  nextSeriesUpdateIndex = 0
			}
		  }
		}

		when (it.type) {
		  REPLACE -> seriesPoints[index] = it.points.toMutableList()
		  APPEND  -> seriesPoints[it.seriesIndex]!!.addAll(
			it.points
		  ) /*might have to create list if this ever throws error*/
		}
		figNextPointsI[index] = 0
	  }
	}
  }
}


class Figure(
  //  var line: Boolean = true
): Pane() {


  fun setup(
	title: String,
	xlabel: String,
	ylabel: String,
	autoY: Boolean,
	yMin: Double,
	yMax: Double,
	seriesCfgs: List<SeriesCfgs>,
	xMin: Double,
	xMax: Double,
	autoX: Boolean
  ) {
	chart.title = title
	(chart.xAxis as DefaultNumericAxis).apply {
	  name = xlabel
	  axisLabel.apply {
		text = xlabel
		fill = Color.WHITE
	  }
	  if (!autoX) {
		max = (xMax)
		min = xMin
	  }
	}
	(chart.yAxis as DefaultNumericAxis).apply {
	  name = ylabel
	  axisLabel.apply {
		text = ylabel
		fill = Color.WHITE
	  }
	  if (!autoY) {
		max = (yMax)
		min = yMin
	  }

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

	chart.axes.forEach { a -> a.forceRedraw() }
	chart.legend.updateLegend(chart.datasets, chart.renderers, true)
	/*if (autoX) {
	  autorangeXWith(xMin, xMax)
	}*/
  }


  fun dataToJson() = JsonArray().apply {
	chart.datasets.forEach { s ->
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
	s += if (marker) {
	  "markerColor: ${seriesColors[i]}; markerType: circle;"
	} else {
	  "markerColor: transparent;"
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

  fun autorangeY() {
	autorangeYWith(*series.values.toTypedArray())
  }

  fun autorangeX() = autorangeXWith(*series.values.toTypedArray())

  fun clear() {
	/*found this method to be the least buggy and fastest, most responsive*/
	series.clear()
	children.remove(chart)
	newChart()
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
	  /*if (scene != null) {
		exactWidthProperty().bind(scene.widthProperty()*0.9)
	  }*/
	  /*sceneProperty().onNonNullChange {
		exactWidthProperty().bind(it.widthProperty()*0.9)
	  }*/
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
