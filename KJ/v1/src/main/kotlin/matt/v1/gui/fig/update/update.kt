package matt.v1.gui.fig.update

import kotlinx.serialization.Serializable
import matt.async.date.sec
import matt.caching.every
import matt.caching.with
import matt.gui.loop.runLaterReturn
import matt.json.toJson
import matt.json.toJsonString
import matt.kjlib.jmath.point.Point
import matt.klib.lang.NEVER
import matt.reflect.NoArgConstructor
import matt.v1.gui.fig.Figure
import matt.v1.gui.fig.update.SeriesUpdateType.APPEND
import matt.v1.gui.fig.update.SeriesUpdateType.REPLACE
import matt.v1.jsonpoint.JsonPoint
import matt.v1.jsonpoint.toJsonPoints
import matt.v1.lab.Experiment.RunStage
import matt.v1.lab.Experiment.RunStage.FIG_COMPLETE
import matt.v1.lab.Experiment.RunStage.WAITING_FOR_FIG
import java.util.concurrent.Semaphore

fun GuiUpdate.jsonString(): String {
  return when (this) {
	is StatusUpdate -> this.toJson().toJsonString()
	is FigureUpdate -> this.toJson().toJsonString()
	is FigUpdate    -> this.toFigUpdate().jsonString()
  }
}

sealed interface GuiUpdate

@NoArgConstructor
@Serializable
class StatusUpdate private constructor(
  var counterName: String? = null,
  var count: Int,
  var total: Int
): GuiUpdate/*, SimpleJson<StatusUpdate>(typekey = null)*/ {

  companion object {
	fun new(
	  counterName: String,
	  count: Int,
	  total: Int,
	) = StatusUpdate(
	  counterName = counterName,
	  count = count,
	  total = total
	)
  }

//  var counterName = counterName
//  var  count by JsonIntProp(count)
//  val total by JsonIntProp(total)
}

interface FigUpdate: GuiUpdate {
  fun toFigUpdate(): FigureUpdate
}


@NoArgConstructor
@Serializable
class FigureUpdate private constructor(
  val updates: MutableList<SeriesUpdate> = mutableListOf()
): FigUpdate {

  companion object {
	fun new(
	  updates: List<SeriesUpdate>
	) = FigureUpdate(updates.toMutableList())
  }

//  val updates by FXList(updates
//	/*builder = object: JsonParser<SeriesUpdate> {
//	  override fun fromJson(jv: JsonElement): SeriesUpdate {
//		return SeriesUpdate.new(REPLACE, 0, listOf(JsonPoint())).apply { loadProperties(jv) }
//	  }
//	},
//	default = updates*/
//  )

  override fun toFigUpdate() = this
  override fun toString(): String {
	var s = "matt.v1.gui.fig.update.FigureUpdate:"
	updates.forEach { s += "\n\t${it}" }
	return s
  }
}

@Serializable
enum class SeriesUpdateType {
  REPLACE, APPEND
}


@NoArgConstructor
@Serializable
class SeriesUpdate private constructor(
  val type: SeriesUpdateType? = null,
  val seriesIndex: Int? = null,
  val points: List<JsonPoint>? = null
): FigUpdate/*, SimpleJson<SeriesUpdate>(typekey = null)*/ {


  companion object {
	fun replaceNextSeries(points: List<Point>) = SeriesUpdate(
	  type = REPLACE, seriesIndex = null, points = points.toJsonPoints()
	)

	infix fun Int.replaceWith(points: List<JsonPoint>) = SeriesUpdate(
	  type = REPLACE, seriesIndex = this, points = points
	)

	fun new(
	  type: SeriesUpdateType,
	  seriesIndex: Int?,
	  points: List<JsonPoint>?
	) = SeriesUpdate(
	  type = type,
	  seriesIndex = seriesIndex,
	  points = points
	)
  }

//  val type by JsonEnumProp(SeriesUpdateType::class, type)
//  val seriesIndex by JsonIntPropN(seriesIndex)
//  val points by JsonJsonListProp(
//	object: JsonParser<JsonPoint> {
//	  override fun fromJson(jv: JsonElement): JsonPoint {
//		return JsonPoint().apply {
//		  loadProperties(jv)
//		}
//	  }
//	},
//	default = points
//  )

  constructor(type: SeriesUpdateType, seriesIndex: Int, x: Double, y: Double): this(
	type = type, seriesIndex = seriesIndex, p = JsonPoint(x = x, y = y)
  )

  constructor(type: SeriesUpdateType, seriesIndex: Int, p: JsonPoint): this(
	type = type, seriesIndex = seriesIndex, points = listOf(p)
  )

  override fun toFigUpdate() = FigureUpdate.new(listOf(this))

  override fun toString(): String {
	var s = "matt.v1.gui.fig.update.SeriesUpdate type=${type}, seriesIndex=${seriesIndex}, points:"
	points?.forEach {
	  s += "\n\t\t${it}"
	}
	return s
  }

}

class FigureUpdater(
  val fig: Figure,
  autoYmin: Boolean,
  autoXmin: Boolean,
  autoYmax: Boolean,
  autoXmax: Boolean,
  getRunStage: ()->RunStage,
  getStopped: ()->Boolean,
  getRunning: ()->Boolean,
  setRunStage: (RunStage)->Unit
) {
  companion object {
	const val FIG_REFRESH_PERIOD_SECS = 0.1
	val figSem = Semaphore(1)
  }

  val seriesPoints = mutableMapOf<Int, MutableList<JsonPoint>>(0 to mutableListOf())
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
		if (figNextPointsI.values.any { it != 0 }) {
		  when {
			autoYmin && autoYmax -> fig.autorangeY()
			autoYmin             -> fig.autorangeYmin()
			autoYmax             -> fig.autorangeYmax()
		  }
		  when {
			autoXmin && autoXmax -> fig.autorangeX()
			autoXmin             -> fig.autorangeXmin()
			autoXmax             -> fig.autorangeXmax()
		  }
		}

//		if (autoY && figNextPointsI.values.any { it != 0 }) fig.autorangeY()
//		if (autoX && figNextPointsI.values.any { it != 0 }) fig.autorangeX()
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

  fun sendPoints(seriesIndex: Int, points: List<JsonPoint>) {
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
		  REPLACE -> seriesPoints[index] = it.points!!.toMutableList()
		  APPEND  -> seriesPoints[it.seriesIndex]!!.addAll(
			it.points!!
		  ) /*might have to create list if this ever throws error*/
		  else -> NEVER
		}
		figNextPointsI[index] = 0
	  }
	}
  }
}