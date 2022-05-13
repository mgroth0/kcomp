package matt.v1.lab

import matt.exec.interapp.waitFor
import matt.gui.loop.runLater
import matt.hurricanefx.eye.lang.BProp
import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.date.tic
import matt.kjlib.file.get
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.ranges.step
import matt.kjlib.str.writeToFile
import matt.kjlib.stream.onEveryIndexed
import matt.klib.log.warn
import matt.klibexport.klibexport.go
import matt.v1.comp.Fit
import matt.v1.comp.Fit.Gaussian
import matt.v1.comp.PoissonVar
import matt.v1.comp.PoissonVar.NONE
import matt.v1.compcache.GaussianFit
import matt.v1.compcache.Point
import matt.v1.compcache.gradient
import matt.v1.compcache.normalizeYToMax
import matt.v1.compcache.normalizeYToMinMax
import matt.v1.compcache.shiftAllByTroughs
import matt.v1.exps.ExpCategory
import matt.v1.gui.fig.FigUpdate
import matt.v1.gui.fig.Figure
import matt.v1.gui.fig.FigureUpdater
import matt.v1.gui.status.StatusLabel
import matt.v1.lab.Experiment.CoreLoop
import matt.v1.lab.Experiment.RunStage.FIG_COMPLETE
import matt.v1.lab.Experiment.RunStage.ITERATING
import matt.v1.lab.Experiment.RunStage.WAITING_FOR_FIG
import matt.v1.lab.petri.Population
import matt.v1.lab.petri.PopulationConfig
import matt.v1.lab.petri.ROSENBERG_TD_C
import matt.v1.lab.petri.pop2D
import matt.v1.model.ComplexCell
import matt.v1.model.PopulationResponse
import matt.v1.model.Response
import matt.v1.model.Stimulus
import org.apfloat.Apfloat
import org.apfloat.Apint


interface SeriesCfgs {
  val label: String
  val line: Boolean
  val markers: Boolean
}

var globalStatusLabel: StatusLabel? = null

data class SeriesCfg(
  val priorWeight: Apfloat? = null,
  val yExtractCustom: (CoreLoop.()->Number)? = null,
  val poissonVar: PoissonVar = NONE,
  val popRcfg: (PopulationResponse.()->PopulationResponse) = { this },
  val stimCfg: CoreLoop.(Stimulus)->Stimulus = { it.copy() },
  val fit: Fit? = null,
  override val label: String,
  override val line: Boolean = true,
  override val markers: Boolean = false,
): SeriesCfgs

data class SeriesCfgV2(
  override val label: String,
  override val line: Boolean = true,
  override val markers: Boolean = false,
): SeriesCfgs


data class Experiment(
  val name: String,
  val title: String,
  val xlabel: String,
  val ylabel: String,
  val xMax: Double = 100.0,
  val xMin: Double = 0.0,
  val yMax: Double = 100.0,
  val yMin: Double = 0.0,
  val autoY: Boolean = false,
  val autoX: Boolean = true,
  val xMetaMin: Apfloat? = null,
  val xMetaMax: Apfloat? = null,
  val metaGain: Boolean = false,
  val statusLabel: StatusLabel,
  val fig: Figure,
  val xStep: Double? = null, /*TO GO*/
  val xOp: CoreLoop.(Apfloat)->Unit = {}, /*TO GO*/
  val series: List<SeriesCfgs>,
  val normToMinMax: Boolean = false,
  val normToMax: Boolean = false,
  val troughShift: Boolean = false,
  val category: ExpCategory,
  val popCfg: PopulationConfig = pop2D,/*TO GO*/
  val uniformW: Apfloat? = null,/*TO GO*/
  val rawInput: Apfloat? = null,/*TO GO*/

  /*ALL TO GO*/
  val attnX0DistMax: Apfloat = 10.0.toApfloat(),
//  val f3dStep: Apfloat = 0.1.toApfloat(), /*base*/
  val f5cStep: Apfloat = 0.2.toApfloat(),

  /*
  *
  * f3cStep = 10.0.toApfloat(),
	fs1Step = 15.0.toApfloat(),
	f3dStep = 0.25.toApfloat(), /*faster*/
  *
  * */


  val f5dStep: Apfloat = 10.0.toApfloat(),
  val fs1Step: Apfloat = 0.25.toApfloat(),
  val decodeCount: Int = 2 /*was 100*/,

  val logForm: Boolean = false,

  val useAttentionMask: Boolean = false,

  val v2: Sequence<FigUpdate>? = null


) {

  val runningProp = BProp(false)
  fun stop() {
	stopped = true
  }

  private var stopped = false
  private val jsonFile = DATA_FOLDER["kcomp"]["v1"]["exps"]["$name.json"]
  val pop by lazy { Population(popCfg) }

  companion object {
	const val CONTRAST1 = 7.5
	const val CONTRAST2 = 20.0
	const val USE_GPPC = true
  }

  fun load() {
	runningProp.value = true
	stopped = false
	if (jsonFile.exists()) {
	  fig.loadJson(jsonFile)
	}
	warn("shouldn't I sometimes respect max y and x here????")
	fig.autorangeY()
	fig.autorangeX()
	warn("shouldn't I sometimes respect max y and x here????")    /*fig.autorangeXWith(
	  if (metaGain) xMetaMin!!.toDouble() else xMin.toDouble(),
	  if (metaGain) xMetaMax!!.toDouble() else xMax.toDouble()
	)*/
	warn("shouldn't I sometimes respect max y and x here????")
	runningProp.value = false
  }


  enum class RunStage {
	ITERATING, WAITING_FOR_FIG, FIG_COMPLETE
  }

  fun run() {
	globalStatusLabel = statusLabel
	var runStage = ITERATING
	val t = tic(prefix = name)
	t.toc("starting experiment")
	runLater { runningProp.value = true }
	stopped = false
	val figUpdater =
	  FigureUpdater(fig = fig, autoY = autoY, autoX = autoX, getRunStage = { runStage }, getStopped = { stopped },
		getRunning = { runningProp.value }, setRunStage = { runStage = it })

	if (v2 != null) {
	  require(series.all { it is SeriesCfgV2 })
	  require(!normToMinMax)
	  require(!normToMax)
	  require(!metaGain)
	  require(!troughShift)
	  require(xMetaMin == null)
	  require(xMetaMax == null)
	  apply {
		v2!!.forEach {
		  figUpdater.update(it.toFigUpdate())
		  if (stopped) return@apply
		}
	  }
	} else {
	  val loop = if (metaGain) {
		MetaLoop(xMetaMin!!..xMetaMax!! step f5dStep)
	  } else buildCoreLoop()

	  fun gaussian(points: List<Point>) = GaussianFit(
		points, xMin = xMin, xStep = ((xMax - xMin)/10), xMax = xMax
	  ).findOrCompute()

	  loop.runExp {
		if (stopped) {
		  return@runExp
		}
		var nextFitIndex = series.size

		@Suppress("UNCHECKED_CAST") val points =
		  if (this is CoreLoop) it as Map<SeriesCfg, MutableList<Point>> else mapOf<SeriesCfg?, MutableList<Point>>(
			(series[0] as SeriesCfg) to (figUpdater.seriesPoints[0]!! + listOf(
			  Point(x = x.toDouble(), y = (it as? Apfloat)?.toDouble() ?: (it as Number).toDouble())
			)).toMutableList()
		  )
		if (troughShift && isLast) points.values.shiftAllByTroughs()
		points.entries.forEachIndexed { i, series ->
		  val resultData =
			(if (normToMinMax) series.value.normalizeYToMinMax() else if (normToMax) series.value.normalizeYToMax() else series.value).toList()
		  figUpdater.sendPoints(
			seriesIndex = i, points = resultData
		  )
		  val g =
			if (isLast && (this@Experiment.series[i] as SeriesCfg).fit == Gaussian && figUpdater.seriesPoints[i]!!.size >= 3) {
			  gaussian(resultData)
			} else null
		  if (g != null) {
			figUpdater.sendPoints(
			  seriesIndex = nextFitIndex, points = g
			)
			nextFitIndex++
		  }
		}
	  }
	}
	runStage = WAITING_FOR_FIG
	if (!stopped) {
	  waitFor(10) { runStage == FIG_COMPLETE }
	  fig.dataToJson().writeToFile(jsonFile)
	}
	runLater { runningProp.value = false }
	val dur = t.toc("finished experiment")
	statusLabel.statusExtra = "experiment complete in $dur"
	waitFor(10) { figUpdater.timerTask.cancelled }
  }


  fun buildCoreLoop(): CoreLoop {
	val range = xMin..xMax step xStep!!
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
		if (extraction != null && i > 0) statusLabel.counters["idk"] = i to itr.size
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
	var popRCfg: (PopulationResponse.()->PopulationResponse) = { this }
	var ti: Apint? = null
	var h: Apfloat? = null
	override fun iteration(): Map<SeriesCfg, MutableList<Point>> {
	  if (itr.indexOf(x) == 0) {
		pop.complexCells.forEach {
		  it.lastResponse = Response(R = 0.0, G_S = 0.0)
		}
	  }    /*coreLoopForStatusUpdates = this*/
	  cell = pop.centralCell
	  stim = pop.cfg.perfectStimFor(cell).copy(a = pop.cfg.baseContrast)
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
		y.popRcfg.go { idk -> popRCfg = idk }
		poissonVar = y.poissonVar
		ser.value += Point(
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
		it.metaC = ROSENBERG_TD_C.toApfloat()*((100.0 - x.toDouble())/100.0).toApfloat()
	  }
	  coreLoop.runExp(extraction = null)
	  return coreLoop.responseSet.values.first().gradient.toApfloat()
	}
  }


}