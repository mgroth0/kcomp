package matt.v1.lab

import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart.Data
import matt.gui.loop.runLater
import matt.hurricanefx.eye.lang.BProp
import matt.kjlib.jmath.sigFigs
import matt.kjlib.log.NEVER
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.prependZeros
import matt.klib.ranges.step
import matt.klibexport.klibexport.setAll
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.lab.Experiment.XVar.AQ
import matt.v1.lab.Experiment.XVar.CONTRAST
import matt.v1.lab.Experiment.XVar.DIST_4_ATTENTION
import matt.v1.lab.Experiment.XVar.MASK
import matt.v1.lab.Experiment.XVar.ORIENTATION
import matt.v1.lab.Experiment.XVar.SIZE
import matt.v1.lab.Experiment.YExtract
import matt.v1.lab.Experiment.YExtract.ASD
import matt.v1.lab.Experiment.YExtract.NO_DN
import matt.v1.lab.Experiment.YExtract.TD
import matt.v1.lab.Experiment.YExtract.WITH_DN
import matt.v1.lab.ResourceUsageCfg.DEV2
import matt.v1.lab.ResourceUsageCfg.FINAL
import matt.v1.model.Cell
import matt.v1.model.ComplexCell
import matt.v1.model.Field
import matt.v1.model.FieldLocAndOrientation
import matt.v1.model.SUPPRESSIVE_FIELD_GAIN_TYPICAL
import matt.v1.model.SimpleCell
import matt.v1.model.SimpleCell.Phase
import matt.v1.model.SimpleCell.Phase.SIN
import matt.v1.model.Stimulus

fun experiments(fig: Figure, statusLabel: StatusLabel): List<Experiment> {


  val baseExp = Experiment(
	name = "3.B",
	title = "DN causes saturation with ↑ contrast",
	xlabel = "% Contrast",
	ylabel = "Neural Response % Maximum",
	fig = fig,
	statusLabel = statusLabel,
	xVar = CONTRAST,
	yExtracts = listOf(NO_DN, WITH_DN),
	normToMaxes = true
  )

  val exps = mutableListOf(baseExp)
  exps += baseExp.copy(
	name = "3.C",
	title = "DN allows suppression",
	xlabel = "Mask % Contrast",
	xMax = 50.0,
	xVar = MASK
  )
  exps += baseExp.copy(
	name = "3.D",
	title = "DN causes tuning curve to peak at optimal size",
	xlabel = "Size (Degrees)",
	xMax = 10.0,
	xVar = SIZE,
	xMin = rCfg.F3D_STEP
  )
  exps += exps.last().copy(
	name = "4.C",
	title = "↓ D.N. in ASD causes ↑ the absolute pop. gain (matches b. data)",
	xMax = 6.05,
	xMin = 1.55,
	yExtracts = listOf(TD, ASD),
	ylabel = "Population Gain",
	autoY = true,
	normToMaxes = false
  )
  exps += exps.last().copy(
	name = "4.D",
	title = "pop. gain from ↓ D.N. in ASD diverges from TD as contrast ↑ (matches b. data)",
	xMin = 0.0,
	xMax = 100.0,
	xVar = CONTRAST,
	xlabel = baseExp.xlabel
  )
  exps += baseExp.copy(
	name = "5.C",
	title = "dist. from center of attn. causes ↓ pop. gain and then ↑ (matches b. data)",
	xMin = 0.0,
	xMax = 10.0,
	xVar = DIST_4_ATTENTION,
	xlabel = "Distance (Degrees)",
	ylabel = exps.last().ylabel,
	troughShift = true,
	autoY = true,
	yExtracts = listOf(TD, ASD),
  )
  exps += exps.last().copy(
	name = "5.D",
	title = "pop. gain gradient from attn increases with ASD symptomatology (↑c) (matches b. data)",
	xMetaMin = 0.0,
	xMetaMax = 50.0,
	xlabel = "Gain Term % Decrease",
	ylabel = "Population Gain Gradient",
	metaGain = true,
	yExtracts = listOf(TD) /*TD gets modified along AQ scale*/
  )

  /*𝑐(𝜃)=𝑐0−𝑤𝜃∙cos(4∙𝜃∙𝜋180⁄),*/
  /*we performed a simulation with𝑐0=1x10−4(the control value of 𝑐) and three values of 𝑤𝜃: 0(reflecting a flat prior), 2.5x10−5(reflecting a weak prior), and 5x10−5(reflecting a strong prior)*/
  exps += baseExp.copy(
	name = "S4.A",
	title = "probability learning stuff (TODO)",
	xMin = 0.0,
	xMax = 180.0,
	xlabel = "Preferred Orientation (Degrees)",
	ylabel = "c(Θ)",
	autoY = true
  )
  return exps
}

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
  val F5D_STEP: Double = 10.0
) {
  FINAL(X0_STEP = 0.2),


  DEV2(
	CELL_THETA_STEP = 10.0,
	CELL_X0_STEP_MULT = 10,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = FINAL.F5C_STEP*2
  ),

  DEBUG(
	CELL_THETA_STEP = 10.0,
	CELL_X0_STEP_MULT = 10,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = 3.0,
	F5D_STEP = 25.0
  )
}

val rCfg = DEV2


const val X0_ABS_MINMAX = 15.0

const val FIELD_SIZE_MULT = 1/*complete guess*/

const val FIELD_ABS_MINMAX = X0_ABS_MINMAX*FIELD_SIZE_MULT


const val THETA_MIN = 0.0
const val THETA_MAX = 179.0

const val REQ_SIZE = 27_180

/*complete guess*/
private val field = Field(absLimXY = FIELD_ABS_MINMAX, stepXY = rCfg.X0_STEP)


data class Response(val x: Double, val r: Double)
data class ResponseSeries(val tag: YExtract) {
  val responses = mutableListOf<Response>()
  val normalizedResponses = mutableListOf<Response>()
}

class ResponseSet(vararg extracts: YExtract) {
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
	val higherTrough = series.map { it.responses }.maxByOrNull { it.minOf { it.r } }!!
	val lowerTrough = series.map { it.responses }.minByOrNull { it.minOf { it.r } }!!
	val diff = higherTrough.minOfOrNull { it.r }!! - lowerTrough.minOfOrNull { it.r }!!
	higherTrough.setAll(higherTrough.map { it.copy(r = it.r - diff) })
  }

  fun popGainGradient(): Double {
	require(series.size == 1)
	val resps = series[0].responses
	return (resps.maxOf { it.r } - resps.minOf { it.r })/(resps.maxOf { it.x } - resps.minOf { it.x })
  }

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
  val yExtracts: List<YExtract>,
  val normToMaxes: Boolean = false,
  val troughShift: Boolean = false
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
	AQ,
	ORIENTATION
  }

  enum class YExtract(val label: String) {
	NO_DN("- D.N"),
	WITH_DN("+ D.N"),
	TD("TD"),
	ASD("ASD"),
  }

  abstract inner class ExperimentalLoop<X, Y>(
	protected val itr: List<X>,
  ) {
	abstract fun iteration(x: X): Y
	var lastX: X? = null
	fun justRun() {
	  itr.forEach { x ->
		if (stopped) return
		lastX = x
		iteration(x)
	  }
	}

	fun runAndExtract(extraction: ExperimentalLoop<X, Y>.(Y)->Unit) {
	  itr.forEach { x ->
		if (stopped) return
		lastX = x
		val y = iteration(x)
		extraction(y)
	  }
	}
  }


  fun setupFig() {
	fig.clear()
	fig.chart.title = title
	fig.chart.xAxis.label = xlabel
	fig.chart.yAxis.label = ylabel
	(fig.chart.xAxis as NumberAxis).upperBound = xMetaMax ?: xMax

	if (!autoY) {
	  (fig.chart.yAxis as NumberAxis).upperBound = yMax
	  (fig.chart.yAxis as NumberAxis).lowerBound = yMin
	}
  }


  fun run() {
	runLater { runningProp.value = true }
	stopped = false
	if (metaGain) {
	  MetaLoop(xMetaMin!!..xMetaMax!! step rCfg.F5D_STEP)
		  .runAndExtract {
			(fig.chart.yAxis as NumberAxis).upperBound = 10.0

			fig.series1.data.add((Data(lastX!!, it)))
			println("current data should be visible: ${fig.series1.data}")
			fig.autorangeY()
		  }
	} else buildCoreLoop().runAndExtract {
	  runLater {
		it.series.forEachIndexed { i, series ->
		  val figSeries = when (i) {
			0    -> fig.series1
			1    -> fig.series2
			else -> NEVER
		  }
		  val resultData = if (normToMaxes) series.normalizedResponses else series.responses
		  figSeries.data.setAll(resultData.map { Data(it.x, it.r) })
		  if (it.series.size > 1) {
			figSeries.name = series.tag.label
		  }
		}



		if (autoY) fig.autorangeY()
	  }
	}
	runLater { runningProp.value = false }
  }

  fun buildCoreLoop(): CoreLoop {
	val aRange = xMin..xMax step when (xVar) {
	  MASK             -> rCfg.F3C_STEP
	  SIZE             -> rCfg.F3D_STEP
	  DIST_4_ATTENTION -> rCfg.F5C_STEP
	  else             -> rCfg.F3B_STEP
	}
	return CoreLoop(aRange.toList(), ResponseSet(*yExtracts.toTypedArray()))
  }

  inner class CoreLoop(
	itr: List<Double>,
	val responseSet: ResponseSet
  ): ExperimentalLoop<Double, ResponseSet>(itr) {


	var tempC: Double? = null

	override fun iteration(x: Double): ResponseSet {

	  var cell = allComplexCells.sortedBy { it.X0 }.first { it.X0 >= 0.0 }
	  var stim = cell.perfectStim().copy(a = .5)
	  var attentionExp = false

	  when (xVar) {
		CONTRAST         -> stim = stim.copy(a = x*0.01)
		MASK             -> stim = stim withMask stim.copy(a = x*0.01, f = stim.f.copy(t = orth(stim.f.t)))
		SIZE             -> stim = stim.copy(a = 1.0, s = x)
		DIST_4_ATTENTION -> {
		  cell = allComplexCells.filter { it.Y0 == 0.0 }.sortedBy { it.X0 }.first { it.X0 >= x }
		  stim = cell.perfectStim().copy(a = 1.0)
		  attentionExp = true
		}
		AQ               -> TODO()
		ORIENTATION      -> TODO()
	  }

	  val DEBUG = if (attentionExp) 250 else 1
	  val S = (allComplexCells - cell).mapIndexed { i, c ->
		if (i%10 == 0) {
		  statusLabel.statusExtra =
			  "stimulating cell ${i.prependZeros(3)}/${allComplexCells.size} with ${xVar.name}: ${x.sigFigs(3).toString().addSpacesUntilLengthIs(5)}"
		}
		val D = c.stimulate(stim, attention = attentionExp)
		val W = cell.weights[c]
		W*D
	  }.sum()*DIRTY_S_MUlT*DEBUG /*without DIRTY_S_MUlT, Fig.3.D doesnt work...nvm still doesnt work*/

	  responseSet.series.forEach {
		it.responses += when (it.tag) {
		  NO_DN   -> Response(
			x,
			cell.stimulate(stim, attention = attentionExp, tempC = tempC)
		  )
		  WITH_DN -> Response(
			x,
			cell.stimulate(stim, divNormS = S, attention = attentionExp, tempC = tempC)
		  )
		  /*above and below same*/
		  TD      -> Response(
			x,
			cell.stimulate(stim, divNormS = S, attention = attentionExp, tempC = tempC)
		  )
		  ASD     -> Response(
			x,
			cell.stimulate(stim, divNormS = S, autism = true, attention = attentionExp, tempC = tempC)
		  )
		}
	  }


	  if (normToMaxes) {
		responseSet.fitToMaxAsPercent()
	  }
	  if (troughShift && x == itr.last()) {
		responseSet.troughShift()
	  }

	  return responseSet


	}
  }


  inner class MetaLoop(itr: List<Double>): ExperimentalLoop<Double, Double>(itr) {
	constructor(r: Iterable<Double>): this(r.toList())

	override fun iteration(x: Double): Double {
	  val tempC = SUPPRESSIVE_FIELD_GAIN_TYPICAL*((100.0 - x)/100)
	  val coreLoop = buildCoreLoop().also { it.tempC = tempC }
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


fun orth(degrees: Double): Double {
  require(degrees in 0.0..180.0)
  return if (degrees < 90.0) degrees + 90.0
  else degrees - 90.0
}
