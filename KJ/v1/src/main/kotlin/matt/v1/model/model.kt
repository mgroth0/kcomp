@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.jmath.AZERO_FLOAT
import matt.kjlib.jmath.div
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.point.BasicPoint
import matt.kjlib.jmath.point.Point
import matt.stream.flatten
import matt.stream.forEachNested
import matt.klib.math.sq
import matt.v1.low.Envelope
import matt.v1.low.PhaseType
import matt.v1.low.PhaseType.COS
import matt.v1.low.Polar
import matt.v1.low.Radians
import matt.v1.low.SamplePhase
import matt.v1.low.SigmaDenominator
import matt.v1.low.XYThetas
import matt.v1.model.field.FieldConfig
import matt.v1.model.field.FieldShape.CON_CIRCLE
import matt.v1.model.field.FieldShape.MAT_CIRCLE
import matt.v1.model.vis.calcVis
import org.apfloat.Apfloat
import kotlin.math.abs

data class Orientation(
  val tDegrees: Double /*THETA_ORIENTATION*/
) {
  val tRadians get() = Radians(tDegrees)()
  fun xyTheta(start: Point, end: Point) = XYThetas(
	tRadians = tRadians, dX = end.xDouble - start.xDouble, dY = end.yDouble - start.yDouble
  )

  val orth by lazy { Orientation(matt.kjlib.jmath.orth(tDegrees)) }
}


interface Subregioned {
  val sx: Double /*SIGMA_WIDTH*/
  val sy: Double /*SIGMA_HEIGHT*/
  val SF: Double
}

private const val CACHE_SIZE = 60_000 /*just over rosenberg's simple cell count*/

val stimCache by lazy { LRUCache<Stimulus, Array<FloatArray>>(CACHE_SIZE) }
val cellCache by lazy { LRUCache<Cell, Array<FloatArray>>(CACHE_SIZE) }

val stimCacheCC by lazy { LRUCache<Stimulus, Array<Float>>(CACHE_SIZE) }
val cellCacheCC by lazy { LRUCache<Cell, Array<Float>>(CACHE_SIZE) }

abstract class FieldGenerator(
  open val fieldCfg: FieldConfig,
  open val fieldLoc: Point,
  val STIM_REC_CC_THETA_STEP: Apfloat
): Point by fieldLoc {


  abstract fun pix(p: BasicPoint): Float?


  val mat by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCache[this]
	  is Cell     -> cellCache[this]
	  else        -> null
	}

	var i = 0
	val m = cache ?: Array(fieldCfg.length) { FloatArray(fieldCfg.length) { Float.NaN } }.let { m ->
	  (0 until fieldCfg.length).forEachNested { x, y ->
		i++
		m[x][y] = pix(
		  BasicPoint(
			x = fieldCfg.fullRange[x], y = fieldCfg.fullRange[y]
		  )
		)!!
	  }
	  m
	}

	if (cache == null) {
	  when (this@FieldGenerator) {
		is Stimulus -> stimCache[this@FieldGenerator] = m
		is Cell     -> cellCache[this@FieldGenerator] = m
	  }
	}
	m    /*}*/

  }

  val concentricCircles by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCacheCC[this]
	  is Cell     -> cellCacheCC[this]
	  else        -> null
	}

	val m: Array<Float> = cache ?: (mutableListOf(pix(BasicPoint(x = 0.0, y = 0.0))!!).apply {
	  fieldCfg.halfRange.forEach { r ->

		val normR = r*(1.0/fieldCfg.sampleStep)
		val circleThetaStep = (STIM_REC_CC_THETA_STEP)/normR

		var a = AZERO_FLOAT

		while (a < Apfloat(360.0)) {
		  val p = pix(Polar(radius = r, rads = Radians(a.toDouble()))())
		  add(p!!)
		  a += circleThetaStep
		}
	  }
	}.toTypedArray())

	if (cache == null) {
	  when (this) {
		is Stimulus -> stimCacheCC[this] = m
		is Cell     -> cellCacheCC[this] = m
	  }
	}
	m
  }


  val maxVis by lazy { vis.maxOf { it.maxOrNull()!! } }
  val minVis by lazy { vis.minOf { it.maxOrNull()!! } }
  val maxAbsVis by lazy { vis.maxOf { it.map { abs(it) }.maxOrNull()!! } }

  /*x y here are percentages like 0.25 might come in and that means 25%*/
  fun getVisSample(x: Double, y: Double, rel: Boolean = false, relWithSign: Boolean = false): Double {

	if (fieldCfg.shape == MAT_CIRCLE) {
	  val absX = x*fieldCfg.fieldAbsMinMax*2 - fieldCfg.fieldAbsMinMax
	  val absY = y*fieldCfg.fieldAbsMinMax*2 - fieldCfg.fieldAbsMinMax
	  if (BasicPoint(x = absX, y = absY) !in fieldCfg.circle) return Double.NaN
	}


	val sX = kotlin.math.min(kotlin.math.floor(x*vis.size).toInt(), vis.size - 1)
	val sY = kotlin.math.min(kotlin.math.floor(y*vis.size).toInt(), vis.size - 1)

	val v = vis[sX][sY]
	if (relWithSign) return v/maxAbsVis
	else return v

	/*return */
  }

  private val vis by lazy { calcVis() }

}

/*probably very inefficient in at least one obvious way*/
class Mult(val f1: FieldGenerator, val f2: FieldGenerator): FieldGenerator(
  fieldCfg = f1.fieldCfg,
  fieldLoc = f1.fieldLoc,
  STIM_REC_CC_THETA_STEP = f1.STIM_REC_CC_THETA_STEP
) {

  init {
	require(f1.fieldCfg == f2.fieldCfg)
	require(f1.fieldLoc == f2.fieldLoc)
  }

  override fun pix(p: BasicPoint): Float? {
	val p1 = f1.pix(p)
	if (p1 != null) {
	  val p2 = f2.pix(p)
	  if (p2 != null) {
		return p1*p2
	  }
	}
	return null
  }

}


sealed interface Input

data class RawInput(val v: Double): Input

data class Stimulus(

  override val fieldCfg: FieldConfig,

  override val fieldLoc: Point,
  val o: Orientation,


  val a: Double, /*ALPHA_CONTRAST*/
  val s: Double, /*SIGMA_SIZE*/
  override val SF: Double,

  val mask: Stimulus? = null,
  val gaussianEnveloped: Boolean = true,

  ): FieldGenerator(
  fieldCfg = fieldCfg,
  fieldLoc = fieldLoc,
  STIM_REC_CC_THETA_STEP = fieldCfg.sampleCCThetaStep
), Subregioned, Input {

  override val sx = s
  override val sy = s

  private val sDen by lazy { 2.0*s.sq() }

  override fun pix(p: BasicPoint): Float? {


	if (fieldCfg.shape == MAT_CIRCLE && p !in fieldCfg.circle) {
	  return null
	}


	val xyt = o.xyTheta(start = this, end = p)


	val envelope = SigmaDenominator(s).let { sd ->
	  Envelope(
		gaussianEnveloped = gaussianEnveloped, xyt = xyt, sigmaX = sd, sigmaY = sd
	  )()/*.apply { if (debug) println("\tenv=${this}") }*/
	}

	val phase = SamplePhase(xT = xyt, SF = SF, phase = COS)()/*.apply { if (debug) println("\tphase=${this}") }*/
	val base = (a*envelope*phase)

	return base.let {
	  if (mask != null) it + mask.pix(p)!! else it
	}.toFloat()
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Subregioned {
  val o: Orientation
  val fieldLoc: Point
}


data class SimpleCell<P: PhaseType>(
  override val fieldCfg: FieldConfig,
  override val fieldLoc: Point,
  override val o: Orientation,
  override val sx: Double, /*SIGMA_WIDTH*/
  override val sy: Double, /*SIGMA_HEIGHT*/
  override val SF: Double,
  val phase: P,
  val gaussianEnveloped: Boolean = true
): FieldGenerator(
  fieldCfg = fieldCfg,
  fieldLoc = fieldLoc,
  STIM_REC_CC_THETA_STEP = fieldCfg.sampleCCThetaStep
), Cell {

  inline fun <reified P: PhaseType> toCellWithPhase(p: P) = SimpleCell(
	fieldLoc = fieldLoc,
	fieldCfg = fieldCfg,
	o = o,
	sx = sx,
	sy = sy,
	SF = SF,
	phase = p,
	gaussianEnveloped = gaussianEnveloped
  )

  /*TODO: should combine with stimulus pix function*/
  override fun pix(p: BasicPoint): Float? {
	if (fieldCfg.shape == MAT_CIRCLE && p !in fieldCfg.circle) return null
	val xyt = o.xyTheta(start = this, end = p)
	return (Envelope(
	  gaussianEnveloped = gaussianEnveloped, xyt = xyt, sigmaX = SigmaDenominator(sx), sigmaY = SigmaDenominator(sy)
	)()*SamplePhase(
	  xT = xyt, SF = SF, phase = phase
	)()).toFloat()
  }


  fun stimulate(stim: Stimulus) =
	if (fieldCfg.shape == CON_CIRCLE) (stim.concentricCircles dot concentricCircles)
	else run {
	  val m1 = stim.mat
	  val flat1 = m1.flatten()
	  val m2 = mat
	  val flat2 = m2.flatten()
	  val dp = flat1 dot flat2
	  dp
	}


}





