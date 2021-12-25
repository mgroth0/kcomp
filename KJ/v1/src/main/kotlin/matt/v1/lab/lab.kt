package matt.v1.lab

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.gsi.chart.axes.spi.DefaultNumericAxis
import javafx.scene.layout.FlowPane
import javafx.scene.paint.Color
import matt.gui.loop.runLater
import matt.hurricanefx.eye.lang.BProp
import matt.json.custom.toJsonWriter
import matt.json.prim.loadJson
import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.file.get
import matt.kjlib.jmath.getPoisson
import matt.kjlib.jmath.logSum
import matt.kjlib.jmath.mean
import matt.kjlib.jmath.orth
import matt.kjlib.jmath.sigFigs
import matt.kjlib.log.err
import matt.kjlib.ranges.step
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.prependZeros
import matt.kjlib.str.taball
import matt.kjlib.stream.onEveryIndexed
import matt.klib.log.warn
import matt.klibexport.klibexport.go
import matt.v1.compcache.BayesianPriorC
import matt.v1.compcache.GPPCUnit
import matt.v1.compcache.GPPCUnit.Companion.ftToSigma
import matt.v1.compcache.GaussianFit
import matt.v1.compcache.LogPoissonPPCUnit
import matt.v1.compcache.MaybePreDNPopR
import matt.v1.compcache.MaybePreDNPopR.Companion.coreLoopForStatusUpdates
import matt.v1.compcache.Point
import matt.v1.compcache.Stimulation
import matt.v1.compcache.gradient
import matt.v1.compcache.normalizedToMinMax
import matt.v1.compcache.shiftAllByTroughs
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.lab.Experiment.CoreLoop
import matt.v1.lab.Experiment.XVar.CONTRAST
import matt.v1.lab.Experiment.XVar.DIST_4_ATTENTION
import matt.v1.lab.Experiment.XVar.MASK
import matt.v1.lab.Experiment.XVar.PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.SIZE
import matt.v1.lab.Experiment.XVar.STIM_AND_PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.STIM_ORIENTATION
import matt.v1.lab.Experiment.XVar.TIME
import matt.v1.lab.Fit.Gaussian
import matt.v1.lab.PoissonVar.FAKE1
import matt.v1.lab.PoissonVar.FAKE10
import matt.v1.lab.PoissonVar.FAKE5
import matt.v1.lab.PoissonVar.NONE
import matt.v1.lab.PoissonVar.YES
import matt.v1.lab.petri.Population
import matt.v1.lab.petri.perfectStim
import matt.v1.lab.petri.pop2D
import matt.v1.lab.rcfg.rCfg
import matt.v1.model.ComplexCell
import matt.v1.model.PopulationResponse
import matt.v1.model.Stimulus
import matt.v1.model.tdDivNorm
import org.apache.commons.math3.exception.ConvergenceException
import java.util.Random
import kotlin.math.max
import kotlin.math.roundToInt

enum class PoissonVar { NONE, YES, FAKE1, FAKE5, FAKE10 }

data class SeriesCfg(
  val label: String,
  val DN: Boolean = false,
  val priorWeight: Double? = null,
  val yExtractCustom: (CoreLoop.()->Double)? = null,
  val poissonVar: PoissonVar = NONE,
  val popRcfg: (PopulationResponse.()->PopulationResponse) = { this },
  val stimCfg: CoreLoop.(Stimulus)->Stimulus = { it.copy() },
  val line: Boolean = true,
  val markers: Boolean = false,
  val fit: Fit? = null
)

enum class ExpCategory {
  ROSENBERG, LOUIE, OTHER;

  val pane by lazy { FlowPane() }
}

enum class Fit {
  Gaussian
}

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
  val xMetaMin: Double? = null,
  val xMetaMax: Double? = null,
  val metaGain: Boolean = false,
  val statusLabel: StatusLabel,
  val fig: Figure,
  val xVar: XVar,
  val xStep: Double,
  val series: List<SeriesCfg>,
  val normToMaxes: Boolean = false,
  val troughShift: Boolean = false,
  val stimTrans: (Stimulus.()->Stimulus)? = null,
  val category: ExpCategory,
  val baseContrast: Double = 0.5,
  val pop: Population = pop2D,
) {


  val runningProp = BProp(false)

  private var stopped = false
  fun stop() {
	stopped = true
  }

  enum class XVar {
	CONTRAST,
	MASK,
	SIZE,
	DIST_4_ATTENTION,
	STIM_ORIENTATION,
	REL_STIM_ORIENTATION,
	PREF_ORIENTATION,
	STIM_AND_PREF_ORIENTATION,
	FT,
	TIME
  }

  companion object {
	val CONTRAST1 = 7.5
	val CONTRAST2 = 20.0
  }


  abstract inner class ExperimentalLoop<X: Any, Y>(
	val itr: List<X>,
  ) {
	lateinit var x: X
	var isLast = false
	protected abstract fun iteration(): Y
	fun justRun() {
	  itr
		.asSequence()
		.forEachIndexed { i, newX ->
		  isLast = i == itr.size - 1
		  if (stopped) return
		  println("i:${i},x:${x},newX:${newX}")
		  x = newX
		  iteration()
		  if (stopped) return
		}
	}

	fun runAndExtract(extraction: ExperimentalLoop<X, Y>.(Y)->Unit) {
	  println("runAndExtract")
	  itr
		.asSequence()
		.onEveryIndexed(10) { i, _ -> if (i > 0) coreLoopForStatusUpdates.update() }
		.forEachIndexed { i, newX ->
		  println("newX:${newX}")
		  isLast = i == itr.size - 1
		  if (stopped) return
		  x = newX
		  val y = iteration()
		  if (stopped) return
		  extraction(y)
		}
	}
  }

  private val jsonFile = DATA_FOLDER["kcomp"]["v1"]["exps"]["$name.json"]


  fun setupFig() {

	fig.clear()
	fig.chart.title = title
	(fig.chart.xAxis as DefaultNumericAxis).apply {
	  name = xlabel
	  axisLabel.apply {
		text = xlabel
		fill = Color.WHITE
	  }
	  max = (xMetaMax ?: xMax).toDouble()
	}
	(fig.chart.yAxis as DefaultNumericAxis).apply {
	  name = ylabel
	  axisLabel.apply {
		text = ylabel
		fill = Color.WHITE
	  }
	  if (!autoY) {
		max = (yMax).toDouble()
		min = yMin.toDouble()
	  }
	}

	series.forEachIndexed { i, s ->
	  fig.debugPrepName = s.label /*fixes bug a few lines below where setting name does nothing*/
	  @Suppress("UNUSED_VARIABLE")
	  val make = fig.series[i]
	  fig.styleSeries(i = i, line = s.line, marker = s.markers)
	}

	var nextFitIndex = series.size
	series.forEach {
	  if (it.fit == Gaussian) {
		fig.debugPrepName = "${it.label} (Gaussian fit)"
		@Suppress("UNUSED_VARIABLE")
		val make = fig.series[nextFitIndex]
		fig.styleSeries(i = nextFitIndex, line = true, marker = false)
		nextFitIndex++
	  }
	}

	fig.chart.axes.forEach { a -> a.forceRedraw() }
	fig.chart.legend.updateLegend(fig.chart.datasets, fig.chart.renderers, true)


  }

  fun load() {
	runningProp.value = true
	stopped = false
	if (jsonFile.exists()) {
	  jsonFile.loadJson(JsonArray::class).forEachIndexed { i, e ->
		val s = fig.series[i]
		e.asJsonArray.forEachIndexed { ii, p ->
		  val point = p.asJsonObject
		  s.set(ii, point["x"].asDouble, point["y"].asDouble)
		}
	  }
	}
	fig.autorangeY()
	fig.autorangeXWith(xMin.toDouble(), xMax.toDouble())
	runningProp.value = false
  }

  fun run() {
	runLater { runningProp.value = true }
	stopped = false
	if (metaGain) {
	  MetaLoop(xMetaMin!!..xMetaMax!! step rCfg.F5D_STEP)
		.runAndExtract {
		  val xForThread = x
		  runLater {
			fig.series[0].add(xForThread.toDouble(), it.toDouble())
		  }
		  var nextFitIndex = 1
		  series.forEachIndexed { i, s ->
			fig.styleSeries(i = i, line = s.line, marker = s.markers)
			val g = if (isLast && s.fit == Gaussian && fig.series[0].xValues.size >= 3) {

			  val diff = xMax - xMin
			  try {
				GaussianFit(
				  fig.series[0].xValues.zip(fig.series[0].yValues).map {
					Point(x = it.first, y = it.second)
				  },
				  xMin = xMin,
				  xStep = (diff/10),
				  xMax = xMax
				).findOrCompute()
			  } catch (e: ConvergenceException) {
				warn(e.message ?: "no message for $e")
				e.printStackTrace()
				listOf()
			  }
			} else null
			runLater {
			  if (g != null) {
				g.forEachIndexed { index, p ->
				  fig.series[nextFitIndex].set(index, p.x.toDouble(), p.y.toDouble())
				  fig.styleSeries(i = nextFitIndex, line = true, marker = false)
				}
				nextFitIndex++
			  }
			}
		  }
		  runLater {
			if (autoY) fig.autorangeY()
			fig.chart.axes.forEach { a -> a.forceRedraw() }
			fig.chart.legend.updateLegend(fig.chart.datasets, fig.chart.renderers, true)
		  }
		}
	} else buildCoreLoop().runAndExtract {
	  var nextFitIndex = series.size
	  if (troughShift && isLast) it.values.shiftAllByTroughs()
	  it.entries.forEachIndexed { i, series ->
		/*using normalizedToMinMax for S1.B since Rosenberg used undocumented normalization there*/
		val resultData = (if (normToMaxes) series.value.normalizedToMinMax() else series.value).toList()

		val g = if (isLast && this@Experiment.series[i].fit == Gaussian && resultData.size >= 3) {

		  val diff = xMax - xMin
		  try {
			GaussianFit(
			  resultData,
			  xMin = xMin,
			  xStep = (diff/10),
			  xMax = xMax
			)
			  .findOrCompute()
		  } catch (e: ConvergenceException) {
			warn(e.message ?: "no message for $e")
			e.printStackTrace()
			listOf()
		  }
		} else null
		runLater {


		  val figSeries = fig.series[i]

		  resultData.forEachIndexed { i, r ->
			/*println("adding point x=${r.x} y=${r.y}")*/
			figSeries.set(i, r.x, r.y)
		  }
		  if (g != null) {
			g.forEachIndexed { index, p ->
			  fig.series[nextFitIndex].set(index, p.x, p.y)
			  fig.styleSeries(i = nextFitIndex, line = true, marker = false)
			}
			nextFitIndex++
		  }
		}


	  }


	  runLater {
		series.forEachIndexed { i, s ->
		  fig.styleSeries(i = i, line = s.line, marker = s.markers)
		}

		if (autoY) fig.autorangeY()
		fig.autorangeXWith(xMin.toDouble(), xMax.toDouble())
		fig.chart.axes.forEach { a -> a.forceRedraw() }
		fig.chart.legend.updateLegend(fig.chart.datasets, fig.chart.renderers, true)
	  }
	}
	if (!stopped) {
	  runLater {
		/*must be in fx thread for now because otherwise theres no way to ensure the datasets are fully filled in time for this*/
		jsonFile.parentFile.mkdirs()
		jsonFile.writeText(JsonArray().apply {
		  fig.chart.datasets.forEach { s ->
			add(JsonArray().apply {
			  (0 until s.dataCount).forEach { index ->
				add(JsonObject().apply {
				  addProperty("x", s[0, index])
				  addProperty("y", s[1, index])
				})
			  }
			})
		  }
		}.toJsonWriter().toJsonString())
	  }

	}
	runLater { runningProp.value = false }
  }

  fun buildCoreLoop(): CoreLoop {
	val aRange = xMin..xMax step xStep
	return CoreLoop(aRange.toList(), series.associateWith { mutableListOf() })
  }

  val attentionExp = xVar == DIST_4_ATTENTION

  inner class CoreLoop(
	itr: List<Double>,
	val responseSet: Map<SeriesCfg, MutableList<Point>>
  ): ExperimentalLoop<Double, Map<SeriesCfg, MutableList<Point>>>(itr) {

	val attentionExp: Boolean = this@Experiment.attentionExp
	val pop = this@Experiment.pop

	var metaC: Double? = null

	internal fun update(
	  verb: String = "stimulating",
	  i: Int? = null
	) {
	  val cellS = if (i != null) "$verb cell ${i.prependZeros(3)}/${pop.complexCells.size} with " else ""
	  statusLabel.statusExtra =
		"${cellS}${xVar.name}: ${
		  x.sigFigs(6)
			.toString()
			.addSpacesUntilLengthIs(8)
		}"
	}

	lateinit var cell: ComplexCell
	lateinit var stim: Stimulus
	lateinit var seriesStim: Stimulus
	var priorW: Double? = null
	lateinit var poissonVar: PoissonVar
	fun ComplexCell.prior() = BayesianPriorC(
	  c0 = tdDivNorm.c,
	  w0 = priorW!!,
	  t = t
	)

	var popRCfg: (PopulationResponse.()->PopulationResponse) = { this }


	fun ComplexCell.cfgStim(
	  cfgStim: Stimulus,
	  popR: PopulationResponse? = null,
	) = Stimulation(
	  cell = this,
	  stim = cfgStim,
	  popR = popR?.copy(
		divNorm = when {
		  metaC != null  -> popR.divNorm.copy(c = metaC!!)
		  priorW != null -> popR.divNorm.copy(c = prior()().also {
			if (this == stupid) {
			  println("priorC=$it")
			}
		  })
		  else           -> popR.divNorm
		}
	  )?.popRCfg(),
	  attention = attentionExp,
	  ti = if (xVar == TIME) itr.indexOf(x) else null,
	  h = if (xVar == TIME) xStep else null,
	).findOrCompute(debug = true)

	val LOG_FORM = false

	fun decodeBeforeGeoMean(
	  ftStim: Stimulus,
	  trialStim: Stimulus,
	) = pop.complexCells
	  .asSequence()
	  .filter {
		/*warnOnce("debugging filter")*/
		/*still flat when all orientations are used*/
		/*it.t == ftStim.t && it.t == trialStim.t*/

		/*&& it.X0 == ftStim.X0 && it.X0 == trialStim.X0

		&& it.Y0 == ftStim.Y0 && it.Y0 == trialStim.Y0*/

		true
	  }.toList()/*.subList(0, 1)*/
	  .mapIndexed { i, c ->
		/*println("cell:$c")*/
		if (i%10 == 0) update(verb = "decoding", i = i + 1)
		val ft = c.cfgStim(
		  ftStim,
		  popR = MaybePreDNPopR(ftStim, attentionExp, pop)()
		)
		//		  warnOnce("debugging ppc")
		val preRI = c.cfgStim(
		  cfgStim = trialStim,
		  popR = MaybePreDNPopR(trialStim, attentionExp, pop)()
		)



		if (LOG_FORM) {
		  val ri = when (poissonVar) {
			NONE   -> preRI.roundToInt()
			YES    -> preRI.getPoisson()
			FAKE1  -> (preRI.roundToInt() + 1)
			FAKE5  -> (preRI.roundToInt() + 5)
			FAKE10 -> (preRI.roundToInt() + 10)
		  }
		  val r = LogPoissonPPCUnit(
			ft = ft,
			ri = ri
		  ).findOrCompute(debug = false)
		  println("ft=$ft")
		  println("ri=$ri")
		  println("r=$r")
		  r
		} else {
		  /*val ri = when (poissonVar) {
			NONE   -> preRI.roundToInt().toApint()
			YES    -> preRI.getPoisson().toApint()
			FAKE1  -> (preRI.roundToInt() + 1).toApint()
			FAKE5  -> (preRI.roundToInt() + 5).toApint()
			FAKE10 -> (preRI.roundToInt() + 10).toApint()
		  }
		  PPCUnit(
			ft = ft.toApfloat(),
			ri = ApfloatMath.round(ri, 20, UNNECESSARY).truncate()
		  ).findOrCompute(debug = false).toDouble()*/


		  val ri = when (poissonVar) {
			NONE   -> preRI
			YES    -> max(preRI + Random().nextGaussian()*ftToSigma(preRI), 0.0)
			FAKE1  -> preRI + 1*ftToSigma(preRI)
			FAKE5  -> preRI + 5*ftToSigma(preRI)
			FAKE10 -> preRI + 10*ftToSigma(preRI)
		  }
		  /*val r = */GPPCUnit(
			ft = ft,
			ri = ri
		  ).findOrCompute(debug = false)
		  /*println("ft=$ft")
		  println("ri=$ri")
		  println("r=$r")
		  r*/
		}

	  }

	fun decode(
	  ftStim: Stimulus,
	  trialStim: Stimulus,
	) = (1..(if (poissonVar == YES) rCfg.DECODE_COUNT else 1)).map {
	  decodeBeforeGeoMean(ftStim = ftStim, trialStim = trialStim).run {
		if (LOG_FORM) sum() else logSum()
	  }
	}.mean()/*.also {
	  println("ftStim=$ftStim")
	  println("trialStim=$trialStim")
	  println("y=$it")
	}*/


	override fun iteration(): Map<SeriesCfg, MutableList<Point>> {

	  if (itr.indexOf(x) == 0) {
		pop.complexCells.forEach {
		  it.xiGiMap.clear()
		  it.xiGiMap.putAll(mapOf(0 to 0.0))
		  it.xiRiMap.clear()
		  it.xiRiMap.putAll(mapOf(0 to 0.0))
		}
	  }

	  MaybePreDNPopR.coreLoopForStatusUpdates = this

	  cell = pop.centralCell
	  stim = cell.perfectStim().copy(a = baseContrast)
	  priorW = null
	  popRCfg = { this }

	  stim = when (xVar) {
		CONTRAST                                               -> stim.copy(a = x*0.01)
		MASK                                                   -> stim withMask stim.copy(
		  a = x*0.01,
		  f = stim.f.copy(t = orth(stim.f.t))
		)
		SIZE                                                   -> stim.copy(s = x)
		in listOf(STIM_ORIENTATION, STIM_AND_PREF_ORIENTATION) -> stim.copy(f = stim.f.copy(t = x))
		else                                                   -> stim
	  }
	  cell = when (xVar) {
		DIST_4_ATTENTION                                       -> pop.complexCells
		  .filter { it.Y0 == 0.0 }
		  .filter { it.X0 == pop.complexCells.filter { it.Y0 == 0.0 }.map { it.X0 }.sorted().first { it >= x } }
		  .filter { it.t == 0.0 }
		  .takeIf { it.count() == 1 }!!
		  .first()
		in listOf(PREF_ORIENTATION, STIM_AND_PREF_ORIENTATION) -> pop.complexCells
		  .filter { it.Y0 == 0.0 }
		  .filter { it.X0 == 0.0 }
		  .filter { it.t == pop.complexCells.map { it.t }.sorted().first { it >= x } }
		  .takeIf {
			val b = it.count() == 1
			if (!b) {
			  println("too many: ${it.count()}")
			  taball(it)
			  err("too many")
			}
			b
		  }!!
		  .first()
		else                                                   -> cell
	  }



	  if (stimTrans != null) stim = stimTrans.invoke(stim)



	  responseSet.forEach { ser ->
		if (stopped) return@iteration responseSet
		val y = ser.key
		seriesStim = stim
		seriesStim = y.stimCfg.invoke(this, seriesStim)
		priorW = y.priorWeight
		/*popRCfg = y.popRCfg*/y.popRcfg.go { idk -> popRCfg = idk }
		poissonVar = y.poissonVar
		ser.value += Point(
		  x,
		  y.yExtractCustom!!.invoke(this)
		)

	  }
	  return responseSet
	}
  }


  inner class MetaLoop(itr: List<Double>): ExperimentalLoop<Double, Double>(itr) {
	constructor(r: Iterable<Double>): this(r.toList())

	override fun iteration(): Double {
	  val coreLoop = buildCoreLoop().also {
		it.metaC = tdDivNorm.c*((100.0 - x)/100)
	  }
	  println("coreLoop.justRun()")
	  coreLoop.justRun()
	  return coreLoop.responseSet.values.first().gradient
	}
  }
}

var stupid: ComplexCell? = null