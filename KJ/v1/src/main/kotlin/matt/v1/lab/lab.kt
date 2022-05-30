package matt.v1.lab

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import matt.async.date.tic
import matt.async.waitFor
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.tornadofx.async.runLater
import matt.json.prim.parseJsonObjs
import matt.kjlib.file.text
import matt.kjlib.file.write
import matt.kjlib.jmath.point.Point
import matt.kjlib.jmath.point.PointDim.Y
import matt.kjlib.jmath.point.gradient
import matt.kjlib.jmath.point.normalizeToMax
import matt.kjlib.jmath.point.normalizeToMinMax
import matt.kjlib.jmath.point.shiftAllByTroughs
import matt.kjlib.jmath.point.toBasicPoints
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.ranges.step
import matt.stream.onEveryIndexed
import matt.klib.commons.get
import matt.klib.lang.err
import matt.klib.lang.go
import matt.v1.V1_DATA_FOLDER
import matt.v1.activity.Response
import matt.v1.cfg.user.UserConfig
import matt.v1.comp.Fit.Gaussian
import matt.v1.comp.PoissonVar
import matt.v1.exps.expmodels.ExpCategory
import matt.v1.figmodels.AxisConfig
import matt.v1.figmodels.SeriesCfg
import matt.v1.figmodels.SeriesCfgV2
import matt.v1.figmodels.SeriesCfgs
import matt.v1.gui.expbox.ExpGui
import matt.v1.gui.fig.update.FigUpdate
import matt.v1.gui.fig.update.FigureUpdate
import matt.v1.gui.fig.update.FigureUpdater
import matt.v1.gui.fig.update.GuiUpdate
import matt.v1.gui.fig.update.StatusUpdate
import matt.v1.gui.fig.update.jsonString
import matt.v1.jsonpoint.JsonPoint
import matt.v1.jsonpoint.toJsonPoints
import matt.v1.lab.Experiment.RunStage.FIG_COMPLETE
import matt.v1.lab.Experiment.RunStage.ITERATING
import matt.v1.lab.Experiment.RunStage.WAITING_FOR_FIG
import matt.v1.lab.petri.Population
import matt.v1.lab.petri.PopulationConfig
import matt.v1.low.GaussianFit
import matt.v1.model.Stimulus
import matt.v1.model.activity.ARI_TD_DN_CFG
import matt.v1.model.complexcell.ComplexCell
import org.apfloat.Apfloat
import org.apfloat.Apint


data class Experiment(
  val name: String,
  val title: String,
  val xAxisConfig: AxisConfig,
  val yAxisConfig: AxisConfig,
  val xMetaMin: Apfloat? = null,
  val xMetaMax: Apfloat? = null,
  val metaGain: Boolean = false,
  val xStep: Double? = null, /*TO GO*/
  val xOp: CoreLoop.(Apfloat)->Unit = {}, /*TO GO*/
  val series: List<SeriesCfgs>,
  val normToMinMax: Boolean = false,
  val normToMax: Boolean = false,
  val troughShift: Boolean = false,
  val category: ExpCategory,
  val popCfg: PopulationConfig? = null,/*TO GO*/
  val uniformW: Apfloat? = null,/*TO GO*/
  val rawInput: Apfloat? = null,/*TO GO*/

  /*ALL TO GO*/
  val attnX0DistMax: Apfloat = 10.0.toApfloat(),
  val f5cStep: Apfloat = 0.2.toApfloat(),

  /*fs1Step = 15.0.toApfloat(),*/


  val f5dStep: Apfloat = 10.0.toApfloat(),
  val fs1Step: Apfloat = 0.25.toApfloat(),
  val decodeCount: Int = 2 /*was 100*/,

  val logForm: Boolean = false,

  val useAttentionMask: Boolean = false,

  val v2: Flow<GuiUpdate>? = null


) {
  init {
	if (xMetaMin != null || xMetaMax != null) {
	  err("this no longer does anything")
	}
  }

  val runningProp = BProp(false)
  fun stop() {
	stopped = true
  }

  private var stopped = false


  lateinit var pop: Population /*neverinit*/

  companion object {
	const val CONTRAST1 = 7.5
	const val CONTRAST2 = 20.0
	const val USE_GPPC = true

	val EXPS_DATA_FOLDER = V1_DATA_FOLDER["exps"]
  }

  private val jsonFile = EXPS_DATA_FOLDER["$name.json"]


  enum class RunStage {
	ITERATING, WAITING_FOR_FIG, FIG_COMPLETE
  }

  fun run(gui: ExpGui, fromJson: Boolean = false) {
	var runStage = ITERATING
	val t = tic(prefix = name)
	t.toc("starting experiment")
	runLater { runningProp.value = true }
	stopped = false
	val figUpdater =
	  FigureUpdater(
		fig = gui.fig,
		autoYmin = yAxisConfig.min == null,
		autoXmin = xAxisConfig.min == null,
		autoYmax = yAxisConfig.max == null,
		autoXmax = xAxisConfig.max == null,
		getRunStage = { runStage },
		getStopped = { stopped },
		getRunning = { runningProp.value },
		setRunStage = { runStage = it }
	  )

	var figUpdates: MutableList<FigureUpdate>? = null
	if (v2 != null) {
	  require(series.all { it is SeriesCfgV2 })
	  require(!normToMinMax)
	  require(!normToMax)
	  require(!metaGain)
	  require(!troughShift)
	  require(xMetaMin == null)
	  require(xMetaMax == null)
	  apply {
		if (fromJson) {
		  if (jsonFile.exists()) {
			gui.statusLabel.statusExtra = "loading experiment"
			jsonFile.text.parseJsonObjs().forEach {
			  val update = Json.decodeFromJsonElement<FigureUpdate>(it)

				/*FigureUpdate.new(listOf()).apply {
				loadProperties(it)
			  }*/
			  figUpdater.update(update)
			  if (stopped) return@apply
			}
		  } else {
			gui.console.println("$jsonFile does not exist")
		  }
		} else {
		  gui.statusLabel.statusExtra = "running experiment"
		  figUpdates = mutableListOf()
		  try {
			runBlocking {
			  v2!!.collect {
				when (it) {
				  is FigUpdate -> {
					it.toFigUpdate().go {
					  figUpdates!! += it
					  figUpdater.update(it)
					}
				  }
				  is StatusUpdate -> gui.statusLabel.counters[it.counterName + ":"] = it.count to it.total
				}
				if (stopped) cancel()
			  }
			}
		  } catch (e: CancellationException) {
			println("job cancelled 123: $e")
		  }
		}
	  }
	} else {
	  val loop = if (metaGain) {
		MetaLoop(xMetaMin!!..xMetaMax!! step f5dStep)
	  } else buildCoreLoop()

	  fun gaussian(points: List<Point>) = GaussianFit(
		points.toBasicPoints(),
		xMin = xAxisConfig.min!!.toDouble(),
		xStep = ((xAxisConfig.max!!.toDouble() - xAxisConfig.min.toDouble())/10),
		xMax = xAxisConfig.max.toDouble()
	  ).findOrCompute()

	  loop.runExp {
		if (stopped) {
		  return@runExp
		}
		var nextFitIndex = series.size

		@Suppress("UNCHECKED_CAST") val points: Map<SeriesCfg, MutableList<Point>> =
		  if (this is CoreLoop) it as Map<SeriesCfg, MutableList<Point>> else mapOf(
			(series[0] as SeriesCfg) to (figUpdater.seriesPoints[0]!! + listOf(
			  JsonPoint(x = x.toDouble(), y = (it as? Apfloat)?.toDouble() ?: (it as Number).toDouble())
			)
				).toMutableList()
		  )
		if (troughShift && isLast) points.values.shiftAllByTroughs()
		points.entries.forEachIndexed { i, series ->
		  val resultData =
			(if (normToMinMax) series.value.normalizeToMinMax(Y) else if (normToMax) series.value.normalizeToMax(
			  Y
			) else series.value).toList()
		  figUpdater.sendPoints(
			seriesIndex = i, points = resultData.toJsonPoints()
		  )
		  val g =
			if (isLast && (this@Experiment.series[i] as SeriesCfg).fit == Gaussian && figUpdater.seriesPoints[i]!!.size >= 3) {
			  gaussian(resultData)
			} else null
		  if (g != null) {
			figUpdater.sendPoints(
			  seriesIndex = nextFitIndex, points = g.toJsonPoints()
			)
			nextFitIndex++
		  }
		}
	  }
	}
	runStage = WAITING_FOR_FIG
	if (!stopped && figUpdates != null && UserConfig.saveExps) {
	  waitFor(10) { runStage == FIG_COMPLETE }
	  jsonFile.write("[" + figUpdates!!.map { it.jsonString() }.joinToString(",") + "]")
	}
	runLater { runningProp.value = false }

	val verb = if (fromJson) "loaded" else "finished"
	val dur = t.toc("$verb experiment")
	gui.statusLabel.statusExtra = "experiment $verb in $dur"
	waitFor(10) { figUpdater.timerTask.cancelled }
  }


  fun buildCoreLoop(): CoreLoop {
	val range = xAxisConfig.min!!.toDouble()..xAxisConfig.max!!.toDouble() step xStep!!
	return CoreLoop(range.toList().map { it.toApfloat() },
	  (series.map { it as SeriesCfg }.associateWith { mutableListOf() })
	)
  }


  abstract inner class ExperimentalLoop<X: Any, Y>(
	val itr: List<X>,
  ) {
	lateinit var x: X
	var isLast = false
	protected abstract fun iteration(): Y
	fun runExp(extraction: (ExperimentalLoop<X, Y>.(Y)->Unit)? = null) {
	  itr.asSequence().onEveryIndexed(10) { i, _ ->
		err("""if (extraction != null && i > 0) statusLabel.counters["idk"] = i to itr.size""")
	  }.forEachIndexed { i, newX ->
		isLast = i == itr.size - 1
		x = newX
		val y = iteration()
		extraction?.go { it.invoke(this, y) }
	  }
	}
  }

  inner class CoreLoop(
	itr: List<Apfloat>, val responseSet: Map<SeriesCfg, MutableList<Point>>
  ): ExperimentalLoop<Apfloat, Map<SeriesCfg, MutableList<Point>>>(itr) {
	val outer = this@Experiment
	var metaC: Apfloat? = null
	lateinit var cell: ComplexCell
	lateinit var stim: Stimulus
	lateinit var seriesStim: Stimulus
	internal var priorW: Apfloat? = null
	lateinit var poissonVar: PoissonVar
	var popRCfg: (Map<ComplexCell, Response>.()->Map<ComplexCell, Response>) = { this }
	var ti: Apint? = null
	var h: Apfloat? = null
	override fun iteration(): Map<SeriesCfg, MutableList<Point>> {
	  if (itr.indexOf(x) == 0) {
		pop.complexCells.forEach {
		  err("it.lastResponse = Response(R = 0.0, G_S = 0.0)")
		}
	  }    /*coreLoopForStatusUpdates = this*/
	  cell = pop.centralCell
	  stim = pop.cfg.perfectStimFor(cell).copy(a = pop.cfg.stimConfig.baseContrast)
	  priorW = null
	  popRCfg = { this }
	  xOp(x)
	  responseSet.forEach { ser ->
		val y = ser.key
		seriesStim = stim
		seriesStim = y.stimCfg.invoke(
		  this, seriesStim
		)
		priorW = y.priorWeight
		y.popRcfg.go { err("idk -> popRCfg = idk") }
		poissonVar = y.poissonVar
		ser.value += JsonPoint(
		  x.toDouble(), y.yExtractCustom!!.invoke(this).toDouble()
		)
	  }
	  return responseSet
	}
  }

  inner class MetaLoop(itr: List<Apfloat>): ExperimentalLoop<Apfloat, Apfloat>(itr) {
	constructor(r: Iterable<Apfloat>): this(r.toList())

	override fun iteration(): Apfloat {
	  val coreLoop = buildCoreLoop().also {
		it.metaC = ARI_TD_DN_CFG.gainC.toApfloat()*((100.0 - x.toDouble())/100.0).toApfloat()
	  }
	  coreLoop.runExp(extraction = null)
	  return coreLoop.responseSet.values.first().gradient.toApfloat()
	}
  }


}