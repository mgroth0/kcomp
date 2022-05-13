package matt.v1.gui.fig.update

import matt.v1.gui.fig.update.SeriesUpdateType.APPEND
import matt.v1.gui.fig.update.SeriesUpdateType.REPLACE
import matt.gui.loop.runLaterReturn
import matt.kjlib.async.every
import matt.kjlib.async.with
import matt.kjlib.date.sec
import matt.v1.compcache.Point
import matt.v1.gui.fig.Figure
import matt.v1.lab.Experiment.RunStage
import matt.v1.lab.Experiment.RunStage.FIG_COMPLETE
import matt.v1.lab.Experiment.RunStage.WAITING_FOR_FIG
import java.util.concurrent.Semaphore

interface FigUpdate {
  fun toFigUpdate(): FigureUpdate

}

fun replaceNextSeries(points: List<Point>) = SeriesUpdate(
  type = REPLACE, seriesIndex = null, points = points
)

class FigureUpdate(
  val updates: List<SeriesUpdate>
): FigUpdate {
  override fun toFigUpdate() = this
  override fun toString(): String {
	var s = "matt.v1.gui.fig.update.FigureUpdate:"
	updates.forEach { s += "\n\t${it}" }
	return s
  }
}

enum class SeriesUpdateType {
  REPLACE, APPEND
}

infix fun Int.replaceWith(points: List<Point>) = SeriesUpdate(
  type = REPLACE, seriesIndex = this, points = points
)

data class SeriesUpdate(
  val type: SeriesUpdateType, val seriesIndex: Int?, val points: List<Point>
): FigUpdate {
  constructor(type: SeriesUpdateType, seriesIndex: Int, x: Double, y: Double): this(
	type = type, seriesIndex = seriesIndex, p = Point(x = x, y = y)
  )

  constructor(type: SeriesUpdateType, seriesIndex: Int, p: Point): this(
	type = type, seriesIndex = seriesIndex, points = listOf(p)
  )

  override fun toFigUpdate() = FigureUpdate(listOf(this))
  override fun toString(): String {
	var s = "matt.v1.gui.fig.update.SeriesUpdate type=${type}, seriesIndex=${seriesIndex}, points:"
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
		  figNextPointsI[i] = seriesPoints[i]!!.size        /*  if (getRunStage() == WAITING_FOR_FIG) {
			  setRunStage(FIG_COMPLETE)
			}*/
		}
		if (autoY && figNextPointsI.values.any { it != 0 }) fig.autorangeY()
		if (autoX && figNextPointsI.values.any { it != 0 }) fig.autorangeX()
		//		fig.chart.axes.forEach { a -> a.forceRedraw() } /*sadly this seems necessary do to internal bugs of the library*/
		/*fig.lessForcefullRedraw()*/ /*sadly this seems necessary do to internal bugs of the library*/
		if (lastLegendUpdate < figNextPointsI.size) {
		  fig.chart!!.legend.updateLegend(fig.chart!!.datasets, fig.chart!!.renderers, true)
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
  fun update(figureUpdate: FigureUpdate) {    /*println("GOT FIGURE UPDATE: ${figureUpdate}")*/
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