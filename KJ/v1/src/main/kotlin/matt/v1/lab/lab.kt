package matt.v1.lab

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.gsi.chart.axes.spi.DefaultNumericAxis
import javafx.scene.paint.Color
import matt.gui.loop.runLater
import matt.hurricanefx.eye.lang.BProp
import matt.json.custom.toJsonWriter
import matt.json.prim.loadJson
import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.file.get
import matt.kjlib.jmath.geometricMean
import matt.kjlib.jmath.getPoisson
import matt.kjlib.jmath.mean
import matt.kjlib.jmath.orth
import matt.kjlib.jmath.sigFigs
import matt.kjlib.log.NEVER
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.prependZeros
import matt.klib.log.warn
import matt.klib.ranges.step
import matt.klibexport.klibexport.setAll
import matt.v1.compcache.BayesianPriorC
import matt.v1.compcache.DivNorm
import matt.v1.compcache.GaussianFit
import matt.v1.compcache.PPCUnit
import matt.v1.compcache.Point
import matt.v1.compcache.PreDNPopR
import matt.v1.compcache.Stimulation
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.lab.Experiment.XVar.CONTRAST
import matt.v1.lab.Experiment.XVar.DIST_4_ATTENTION
import matt.v1.lab.Experiment.XVar.MASK
import matt.v1.lab.Experiment.XVar.PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.SIZE
import matt.v1.lab.Experiment.XVar.STIM_AND_PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.STIM_ORIENTATION
import matt.v1.lab.Fit.Gaussian
import matt.v1.lab.PoissonVar.FAKE1
import matt.v1.lab.PoissonVar.FAKE10
import matt.v1.lab.PoissonVar.FAKE5
import matt.v1.lab.PoissonVar.NONE
import matt.v1.lab.PoissonVar.YES
import matt.v1.lab.ResourceUsageCfg.DEV
import matt.v1.lab.ResourceUsageCfg.FINAL
import matt.v1.lab.YExtract.CCW
import matt.v1.lab.YExtract.MAX_PPC
import matt.v1.lab.YExtract.POP_GAIN
import matt.v1.lab.YExtract.PPC
import matt.v1.lab.YExtract.PPC_UNIT
import matt.v1.lab.YExtract.PRIOR
import matt.v1.model.BASE_SIGMA_POOLING
import matt.v1.model.Cell
import matt.v1.model.ComplexCell
import matt.v1.model.Field
import matt.v1.model.FieldLocAndOrientation
import matt.v1.model.PopulationResponse
import matt.v1.model.SimpleCell
import matt.v1.model.SimpleCell.Phase
import matt.v1.model.SimpleCell.Phase.SIN
import matt.v1.model.Stimulus
import matt.v1.model.tdDivNorm
import org.apache.commons.math3.exception.ConvergenceException
import kotlin.math.abs
import kotlin.math.roundToInt


private const val BASE_SIGMA_STEP = 0.1
private const val FASTER_SIGMA_STEP = 0.25

enum class ResourceUsageCfg(
  val X0_STEP: Double = 0.2,
  val CELL_THETA_STEP: Double = 1.0,
  val CELL_X0_STEP_MULT: Int = 1,
  val F3B_STEP: Double = 1.0,
  val F3C_STEP: Double = 0.005,
  val F3D_STEP: Double = BASE_SIGMA_STEP,
  val F5C_STEP: Double = 0.2,
  val F5D_STEP: Double = 10.0,
  val FS1_STEP: Double = 0.25,
) {
  FINAL(X0_STEP = 0.2),


  DEV(
	CELL_THETA_STEP = 10.0,
	CELL_X0_STEP_MULT = 10,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = FINAL.F5C_STEP*2,
	FS1_STEP = 10.0,
  ),

  @Suppress("unused")
  DEBUG(
	CELL_THETA_STEP = 30.0,
	CELL_X0_STEP_MULT = 40,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = 3.0,
	F5D_STEP = 25.0
  )
}

val rCfg = DEV


const val X0_ABS_MINMAX = 15.0

const val FIELD_SIZE_MULT = 1/*complete guess*/

const val FIELD_ABS_MINMAX = X0_ABS_MINMAX*FIELD_SIZE_MULT


const val THETA_MIN = 0.0
const val THETA_MAX = 179.0

const val REQ_SIZE = 27_180

/*complete guess*/
private val field = Field(absLimXY = FIELD_ABS_MINMAX, stepXY = rCfg.X0_STEP)


data class Response(val x: Double, val r: Double)
data class ResponseSeries(val yExtract: SeriesCfg) {
  val responses = mutableListOf<Response>()
  val normalizedResponses = mutableListOf<Response>()
}

class ResponseSet(vararg extracts: SeriesCfg) {
  val series = extracts.map { ResponseSeries(it) }
  fun fitToMaxAsPercent() {
	series.forEachIndexed { i, series ->
	  val max = series.responses.maxOf { it.r }
	  if (i == 0) {
		series.normalizedResponses.setAll(series.responses.map { it.copy(r = it.r/max*100) })
	  } else {
		series.normalizedResponses.setAll(series.responses.map { it.copy(r = it.r/max*100) })
	  }
	}
  }

  fun troughShift() {
	val higherTrough = series.map { it.responses }.maxByOrNull { it.minOf { resp -> resp.r } }!!
	val lowerTrough = series.map { it.responses }.minByOrNull { it.minOf { resp -> resp.r } }!!
	val diff = higherTrough.minOfOrNull { it.r }!! - lowerTrough.minOfOrNull { it.r }!!
	higherTrough.setAll(higherTrough.map { it.copy(r = it.r - diff) })
  }

  fun popGainGradient(): Double {
	require(series.size == 1)
	val resps = series[0].responses
	return (resps.maxOf { it.r } - resps.minOf { it.r })/(resps.maxOf { it.x } - resps.minOf { it.x })
  }

}

enum class YExtract {
  PRIOR,
  PPC, /*Probabilistic Population Code*/
  POP_GAIN, /*POP_GAIN includes other stuff?*/
  MAX_PPC,
  CCW,
  PPC_UNIT
}

enum class PoissonVar {
  NONE, YES, FAKE1, FAKE5, FAKE10
}

data class SeriesCfg(
  val label: String,
  val DN: Boolean = false,
  val contrastAlpha: Double? = null,
  val priorWeight: Double? = null,
  val sigmaPooling: Double = BASE_SIGMA_POOLING,
  val yExtract: YExtract = POP_GAIN,
  val poissonVar: PoissonVar = NONE,
  val poissonVarI: Int? = null,
  val divNorm: DivNorm = tdDivNorm,
  val stimTheta: Double? = null,
  val stimGaussianEnveloped: Boolean? = null,
  val stimSF: Double? = null,
  val relThetaMid: Double? = null,
  val line: Boolean = true,
  val markers: Boolean = false,
  val fit: Fit? = null
) {
  init {
	require(line || markers)
  }
}

enum class ExpCategory {
  ROSENBERG, OTHER
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
	FT
  }

  companion object {
	const val CONTRAST1 = 7.5
	const val CONTRAST2 = 20.0
  }


  abstract inner class ExperimentalLoop<X: Any, Y>(
	protected val itr: List<X>,
  ) {
	lateinit var x: X
	var isLast = false
	protected abstract fun iteration(): Y
	fun justRun() {
	  itr.forEachIndexed { i, newX ->
		isLast = i == itr.size - 1
		if (stopped) return
		x = newX
		iteration()
		if (stopped) return
	  }
	}

	fun runAndExtract(extraction: ExperimentalLoop<X, Y>.(Y)->Unit) {
	  itr.forEachIndexed { i, newX ->
		isLast = i == itr.size - 1
		if (stopped) return
		x = newX
		val y = iteration()
		if (stopped) return
		extraction(y)
	  }
	}
  }

  val jsonFile = DATA_FOLDER["kcomp"]["v1"]["exps"]["$name.json"]


  fun setupFig() {

	fig.clear()
	//	GaussianCurveFitter.create().fit()
	fig.chart.title = title
	(fig.chart.xAxis as DefaultNumericAxis).apply {
	  name = xlabel
	  axisLabel.apply {
		text = xlabel
		fill = Color.WHITE
	  }
	  max = xMetaMax ?: xMax
	}
	(fig.chart.yAxis as DefaultNumericAxis).apply {
	  name = ylabel
	  axisLabel.apply {
		text = ylabel
		fill = Color.WHITE
	  }
	  if (!autoY) {
		max = yMax
		min = yMin
	  }
	}

	series.forEachIndexed { i, s ->
	  fig.debugPrepName = s.label /*fixes bug a few lines below where setting name does nothing*/
	  @Suppress("UNUSED_VARIABLE")
	  val make = fig.series[i]
	  fig.styleSeries(i = i, line = s.line, marker = s.markers)
	  //	  fig.setSeriesShowLine(i, s.line)
	  //	  fig.setSeriesShowMarkers(i, s.markers)
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
	fig.autorangeXWith(xMin, xMax)
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
			  fig.series[0].add(xForThread, it)
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
					fig.series[nextFitIndex].set(index, p.x, p.y)
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
	  it.series.forEachIndexed { i, series ->
		val resultData = (if (normToMaxes) series.normalizedResponses else series.responses).toList()

		val g = if (isLast && this@Experiment.series[i].fit == Gaussian && resultData.size >= 3) {

		  val diff = xMax - xMin
		  try {
			GaussianFit(
			  resultData.map { Point(x = it.x, y = it.r) },
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
			figSeries.set(i, r.x, r.r)
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
		fig.autorangeXWith(xMin, xMax)
		fig.chart.axes.forEach { a -> a.forceRedraw() }
		fig.chart.legend.updateLegend(fig.chart.datasets, fig.chart.renderers, true)
	  }
	}
	if (!stopped){
	  runLater{
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
	return CoreLoop(aRange.toList(), ResponseSet(*series.toTypedArray()))
  }

  inner class CoreLoop(
	itr: List<Double>,
	val responseSet: ResponseSet
  ): ExperimentalLoop<Double, ResponseSet>(itr) {


	var metaC: Double? = null

	internal fun update(
	  verb: String = "stimulating",
	  i: Int
	) {
	  statusLabel.statusExtra =
		  "$verb cell ${i.prependZeros(3)}/${allComplexCells.size} with ${xVar.name}: ${
			x.sigFigs(3)
				.toString()
				.addSpacesUntilLengthIs(5)
		  }"
	}

	override fun iteration(): ResponseSet {

	  PreDNPopR.coreLoopForStatusUpdates = this

	  var cell = allComplexCells.sortedBy { it.X0 }.first { it.X0 >= 0.0 }
	  var stim = cell.perfectStim().copy(a = baseContrast)

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
		DIST_4_ATTENTION                                       -> allComplexCells.sortedBy { it.X0 }
			.first { it.X0 >= x }
		in listOf(PREF_ORIENTATION, STIM_AND_PREF_ORIENTATION) -> allComplexCells.sortedBy { it.t }
			.filter { it.t >= x }
			.sortedBy { it.X0 }
			.first { it.X0 >= 0.0 }
		else                                                   -> cell
	  }

	  /* val fe = when (xVar) {
		 FT   -> x
		 else -> 0.1
	   }*/

	  val attentionExp = xVar == DIST_4_ATTENTION

	  if (stimTrans != null) stim = stimTrans.invoke(stim)


	  var priorW: Double? = null
	  fun ComplexCell.prior() = BayesianPriorC(
		c0 = tdDivNorm.c,
		w0 = priorW!!,
		t = t
	  )

	  fun ComplexCell.cfgStim(
		cfgStim: Stimulus,
		popR: PopulationResponse? = null
	  ) = Stimulation(
		cell = this,
		stim = cfgStim,
		popR = popR?.copy(
		  divNorm = if (metaC != null) popR.divNorm.copy(c = metaC!!) else if (priorW != null) popR.divNorm.copy(c = prior().findOrCompute()) else popR.divNorm
		),
		attention = attentionExp
	  ).findOrCompute(debug = false)


	  /*val pt = (1.0/(THETA_MAX - THETA_MIN))*/

	  /*needed for geo mean*/
	  /*val DEBUG_PPC = 1*10.0.pow(16) *//*because the formula they gave was a proportion, this is legal*/

	  /*higher = more lenient*/
	  /*	  val THETA_THRESHOLD = 1000
			val DIST_THRESHOLD = 1000*/




	  fun decode(
		ftStim: Stimulus,
		trialStim: Stimulus,
		poisson: PoissonVar,
		popRcfg: PopulationResponse.()->PopulationResponse
	  ) = allComplexCells
		  .asSequence()
		  .mapIndexed { i, c ->
			/*.parMapIndexed { i, c ->*/
			if (i%10 == 0) update(verb = "decoding", i = i + 1)
			val ft = c.cfgStim(
			  ftStim,
			  popR = PreDNPopR(ftStim, attentionExp).findOrCompute().popRcfg()
			)
			/*println("c.X0=${c.X0},c.t=${c.t},ftStim.t=${ftStim.t},ft=${ft}")*/

			val preRI = c.cfgStim(
			  cfgStim = trialStim,
			  popR = PreDNPopR(trialStim, attentionExp).findOrCompute().popRcfg()
			)
			/*println("c.X0=${c.X0},c.t=${c.t},trialStim.t=${trialStim.t},preRI=${preRI}")*/

			val ri = when (poisson) {
			  NONE   -> preRI.roundToInt()
			  YES    -> preRI.getPoisson()
			  FAKE1  -> preRI.roundToInt() + 1
			  FAKE5  -> preRI.roundToInt() + 5
			  FAKE10 -> preRI.roundToInt() + 10
			}
			(PPCUnit(
			  ft = ft,
			  ri = ri
			).findOrCompute(debug = false)) /* * DEBUG_PPC (needed for geo mean)*/
		  }.let {
			when (poisson) {
			  FAKE1  -> it.mean()
			  FAKE5  -> it.mean()
			  FAKE10 -> it.mean()
			  else   -> it.geometricMean()
			}
		  }
	  /*How do you add weights to elements in a product?*/
	  /*pt include in any discussions, not in computation. PPC is a proportion so this is legal*/



	  responseSet.series.forEach { ser ->
		if (stopped) return@iteration responseSet
		val y = ser.yExtract
		var rStim = y.contrastAlpha?.let { a -> stim.copy(a = a) } ?: stim
		rStim = y.stimTheta?.let { t -> rStim.copy(f = rStim.f.copy(t = t)) } ?: rStim
		rStim = y.stimGaussianEnveloped?.let { g -> rStim.copy(gaussianEnveloped = g) } ?: rStim
		rStim = y.stimSF?.let { sf -> rStim.copy(SF = sf) } ?: rStim

		if (y.relThetaMid != null) {
		  rStim = rStim.copy(f = rStim.f.copy(t = y.relThetaMid + x))
		}

		priorW = y.priorWeight

		val popRcfg: PopulationResponse.()->PopulationResponse = {
		  copy(
			sigmaPooling = y.sigmaPooling,
			divNorm = y.divNorm
		  )
		}
		val ccwTrials = when (ser.yExtract.poissonVar) {
		  in listOf(NONE, FAKE1, FAKE5, FAKE10) -> 1
		  YES                                   -> 50
		  //		  FAKE -> NEVER
		  else                                  -> NEVER
		}
		ser.responses += Response(
		  x,
		  when (y.yExtract) {
			PRIOR    -> cell.prior().findOrCompute()
			PPC      -> decode(
			  ftStim = rStim,
			  poisson = ser.yExtract.poissonVar,
			  trialStim = rStim.copy(f = rStim.f.copy(t = 90.0)),
			  popRcfg = popRcfg
			)
			MAX_PPC  -> (1..(if (y.poissonVar == NONE) 1 else 20)).map {
			  decode(
				ftStim = rStim,
				poisson = ser.yExtract.poissonVar,
				trialStim = rStim,
				popRcfg = popRcfg
			  )
			}.mean()/*.also {
			  *//*println("decoded:${it}")   *//*
			}*/
			CCW      -> {
			  val substep = 1.0
			  (1..ccwTrials).map {
				val probs = (y.relThetaMid!! - (substep*9)..y.relThetaMid + substep*10 step substep)
					.mapIndexed { i, theta ->
					  (decode(
						ftStim = rStim.copy(f = rStim.f.copy(t = theta)),
						poisson = ser.yExtract.poissonVar,
						trialStim = rStim,
						popRcfg = popRcfg
					  ) to if (theta > y.relThetaMid) 1.0 else 0.0)/*.also {
						println("${y.label} decoded(ts=${theta}/${rStim.t}):${it}")
					  }*/
					}
				/*when (ser.yExtract.poissonVar) {
				  in listOf(NONE, FAKE) -> (probs.filter { it.second == 1.0 }
												.meanOf { it.first.alsoPrintln { "1p:${this}" } } - probs.filter { it.second == 0.0 }
												.meanOf { it.first.alsoPrintln { "0p:${this}" } }) + 0.5
				  YES                   -> probs.maxByOrNull { it.first }!!.second
				  else                  -> NEVER
				  *//*FAKE -> NEVER*//*
				}*/


				probs.maxByOrNull { it.first }!!.second

				/*WIth this, all three (deterministic,poisson,and fake) have 100% perfect results with no diff between 45 and 90*/
				/*listOf(
				  probs.filter { it.second == 0.0 }.meanOf { it.first.alsoPrintln { "0p:${this}" } },
				  probs.filter { it.second == 1.0 }.meanOf { it.first.alsoPrintln { "1p:${this}" } },
				).mapIndexed { i, m -> m to i }
					.maxByOrNull { it.first }!!.second.toDouble()*/
			  }.mean()
			}
			POP_GAIN -> cell.cfgStim(
			  cfgStim = rStim,
			  popR = if (y.DN) PreDNPopR(rStim, attentionExp).findOrCompute().popRcfg() else null
			)
			PPC_UNIT -> {
			  /*var nextX = 0.0
			  var lastResult = -1.0
			  val step = 0.1
			  do {
				nextX += step*/
			  /*val thisResult = */PPCUnit(x, (x + y.poissonVarI!!).roundToInt()).findOrCompute()
			  /*	  println(
					  "x=${x},nextX=${nextX.sigFigs(3)},ri=${(nextX + fe).roundToInt()},thisResult=${
						thisResult.sigFigs(
						  3
						)
					  }"
					)*/
			  /*		val stillHigher = thisResult > lastResult
					  if (stillHigher) {
						lastResult = thisResult
					  }
					} while (stillHigher)
					nextX - step*/
			}
		  }
		)
	  }

	  if (normToMaxes) responseSet.fitToMaxAsPercent()
	  if (troughShift && x == itr.last()) responseSet.troughShift()

	  return responseSet
	}
  }


  inner class MetaLoop(itr: List<Double>): ExperimentalLoop<Double, Double>(itr) {
	constructor(r: Iterable<Double>): this(r.toList())

	override fun iteration(): Double {
	  val coreLoop = buildCoreLoop().also {
		it.metaC = tdDivNorm.c*((100.0 - x)/100)
	  }
	  coreLoop.justRun()
	  return coreLoop.responseSet.popGainGradient()
	}
  }
}


val baseField = FieldLocAndOrientation(
  t = THETA_MIN,
  X0 = -X0_ABS_MINMAX,
  Y0 = 0.0,
  field = field
)

val baseStim = Stimulus(
  f = baseField,
  a = 0.5,
  s = 1.55,
  SF = 5.75,
)


val baseSimpleSinCell = SimpleCell(
  f = baseField,


  sx = 0.7,
  sy = 1.2,
  /*Rosenberg used an elliptical receptive field without explanation. This may interfere with some orientation-based results*/
  /*sx = 0.95,
  sy = 0.95,*/

  SF = 4.0,
  phase = SIN
)

val allSimpleSinCells = (-X0_ABS_MINMAX..X0_ABS_MINMAX step (rCfg.X0_STEP*rCfg.CELL_X0_STEP_MULT))
	.flatMap { x0 ->
	  (THETA_MIN..THETA_MAX step rCfg.CELL_THETA_STEP).map { t ->
		baseSimpleSinCell.copy(f = baseSimpleSinCell.f.copy(X0 = x0, t = t))
	  }
	}.also {
	  if (rCfg == FINAL) {
		require(it.size == REQ_SIZE) { "size is ${it.size} but should be $REQ_SIZE" }
	  }

	}
val allSimpleCosCells = allSimpleSinCells.map {
  it.copy(phase = Phase.COS)
}
val allComplexCells = allSimpleSinCells.zip(allSimpleCosCells).map { ComplexCell(it) }
val DIRTY_S_MUlT = REQ_SIZE.toDouble()/allComplexCells.size.toDouble()

fun Cell.perfectStim() = baseStim.copy(f = baseStim.f.copy(t = t, X0 = X0, Y0 = Y0))


/**
 * Shortest distance (angular) between two angles.
 * It will be in range [0, 180].
 */
fun angularDifference(alpha: Double, beta: Double): Double {
  val phi = abs(beta - alpha)%360.0  /*This is either the distance or 360 - distance*/
  return if (phi > 180.0) 360.0 - phi else phi
}

