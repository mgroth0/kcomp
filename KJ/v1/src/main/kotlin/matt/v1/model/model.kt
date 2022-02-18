@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.jmath.div
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.jmath.unaryMinus
import matt.kjlib.log.NEVER
import matt.kjlib.log.err
import matt.kjlib.ranges.step
import matt.kjlib.stream.flatten
import matt.kjlib.stream.forEachNested
import matt.klib.dmap.withStoringDefault
import matt.klib.math.sq
import matt.reflect.toStringBuilder
import matt.v1.compcache.APoint
import matt.v1.compcache.AttentionAmp
import matt.v1.compcache.DivNorm
import matt.v1.compcache.Envelope
import matt.v1.compcache.Norm
import matt.v1.compcache.Point
import matt.v1.compcache.Polar
import matt.v1.compcache.Radians
import matt.v1.compcache.SamplePhase
import matt.v1.compcache.SigmaDenominator
import matt.v1.compcache.Weight
import matt.v1.compcache.XYThetas
import matt.v1.lab.petri.PopulationConfig
import matt.v1.model.NormMethod.ADD_POINT_5
import matt.v1.model.NormMethod.RATIO
import matt.v1.model.Phase.COS
import matt.v1.model.Phase.SIN
import matt.v1.scaling.requireIsSafe
import org.apfloat.Apfloat
import org.apfloat.Apint
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess

val ZERO = 0.0/*.toFloat()*/
val AZERO = Apfloat.ZERO
val ONE = 1.0/*.toFloat()*/
val AONE = Apfloat.ONE

/*just for visualizing*/
enum class NormMethod {
  RATIO, ADD_POINT_5
}

val NORM = ADD_POINT_5


data class Field(
  val absLimXY: Double, val sampleStep: Double
) {
  val fullRange = (-absLimXY..absLimXY step sampleStep).toList()
  val halfRange by lazy { (sampleStep..absLimXY step sampleStep) }
  val length = fullRange.size
  val circle = Circle(radius = absLimXY)
  val visCircle = Circle(radius = absLimXY, Point(x = absLimXY/2, y = absLimXY/2))

}

private var DEBUG_PRINTED = false


interface FieldLocI: HasFieldStupid {


  val X0: Double /*HORIZONTAL_POS*/
  val Y0: Double /*VERTICAL_POS*/


  /*TODO: better naming*/
  infix fun normTo(other: FieldLocI) = Norm(
	dX = X0 - other.X0, dY = Y0 - other.Y0
  )

  infix fun normDistTo(other: FieldLocI) = normTo(other).findOrCompute()
}

interface HasFieldStupid {
  val field: Field /*HEIGHT_WIDTH*/
}

data class FieldLocAndOrientation(
  override val tDegrees: Double, override val X0: Double, override val Y0: Double, override val field: Field
): Orientation, HasFieldStupid

interface Orientation: FieldLocI {
  val tDegrees: Double /*THETA_ORIENTATION*/
  val tRadians get() = Radians(tDegrees)()
  fun xyTheta(x: Double, y: Double) = XYThetas(
	tRadians = tRadians, dX = x - X0, dY = y - Y0
  )/*
	fun yTheta(x: Double, y: Double) = YTheta(
	  tRadians = tRadians,
	  dX = x - X0,
	  dY = y - Y0
	)*/
}

interface Subregioned: FieldLocI {
  val sx: Double /*SIGMA_WIDTH*/
  val sy: Double /*SIGMA_HEIGHT*/
  val SF: Double
}

private const val CACHE_SIZE = 30_000 /*just over rosenberg's cell count*/

val stimCache = LRUCache<Stimulus, Array<FloatArray>>(CACHE_SIZE)
val cellCache = LRUCache<Cell, Array<FloatArray>>(CACHE_SIZE)

val stimCacheCC = LRUCache<Stimulus, Array<Float>>(CACHE_SIZE)
val cellCacheCC = LRUCache<Cell, Array<Float>>(CACHE_SIZE)

var debugGoodToGo = false

abstract class FieldGenerator(
  open val popCfg: PopulationConfig, override val field: Field, val STIM_REC_CC_THETA_STEP: Apfloat
): HasFieldStupid {

  abstract fun pix(p: Point): Float?


  val mat by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCache[this]
	  is Cell     -> cellCache[this]
	  else        -> null
	}

	/*println("field.length=${field.length} for ${this.hashCode()}")*/

	/*val m = cache ?: mk.empty<Apfloat, D2>(field.length, field.length).let { m ->
	  (0 until field.length).forEachNested { x, y ->
		m[x, y] = pix(APoint(x = field.range[x], y = field.range[y]))
	  }
	  m
	}*/    /*val t = tic(prefix = "getting m")*/    /*t.sampleEveryByPrefix(100, onlyIf = this is SimpleCell) {*/
	var i = 0
	val m = cache ?: Array(field.length) { FloatArray(field.length) { Float.NaN } }.let { m ->
	  (0 until field.length).forEachNested { x, y ->
		i++
		m[x][y] = pix(
		  Point(
			x = field.fullRange[x], y = field.fullRange[y]
		  )/*, sampleTic = this@FieldGenerator is SimpleCell && t.enabled && (i%10000 == 0)*/
		)!!
	  }
	  m
	}    /*t.toc("got m (shape = ${m.size},${m[0].size})")*/

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

	val m: Array<Float> = cache ?: (mutableListOf(pix(Point(x = 0.0, y = 0.0))!!).apply {
	  field.halfRange.forEach { r ->

		val normR = r*(1.0/field.sampleStep)
		val circleThetaStep = (STIM_REC_CC_THETA_STEP)/normR

		var a = Apfloat(AZERO.toDouble())        /*if (this@FieldGenerator is Stimulus && X0 == 0.0 && Y0 == 0.0 && t in listOf(45.0, 90.0) && r ==1.0) {

		}*/

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
	if (!DEBUG_PRINTED) {
	  println("concentricCircles.size=${m.size}")
	  DEBUG_PRINTED = true
	}
	m
  }


  val maxVis by lazy { vis.maxOf { it.maxOrNull()!! } }
  val minVis by lazy { vis.minOf { it.maxOrNull()!! } }
  val maxAbsVis by lazy { vis.maxOf { it.map { abs(it) }.maxOrNull()!! } }

  /*x y here are percentages like 0.25 might come in and that means 25%*/
  fun getVisSample(x: Double, y: Double, rel: Boolean = false, relWithSign: Boolean = false): Double {

	if (popCfg.matCircles) {
	  val absX = x*field.absLimXY*2 - field.absLimXY
	  val absY = y*field.absLimXY*2 - field.absLimXY
	  if (Point(x = absX, y = absY) !in field.circle) return Double.NaN
	}


	val sX = kotlin.math.min(kotlin.math.floor(x*vis.size).toInt(), vis.size - 1)
	val sY = kotlin.math.min(kotlin.math.floor(y*vis.size).toInt(), vis.size - 1)

	val v = vis[sX][sY]
	if (relWithSign) return v/maxAbsVis
	else return v

	/*return */
  }

  private val vis by lazy {
	val v = (0 until field.length).map { DoubleArray(field.length) }.toTypedArray()
	when (NORM) {
	  RATIO       -> {
		err("No")
		val matMin by lazy {
		  mat.flatten().filter { it != null }.minOf { it!!.toDouble() }

		}
		val matMax by lazy { mat.flatten().filter { it != null }.maxOf { it!!.toDouble() } }
		if (matMin == matMax) {
		  println("stopping because min equals max")
		  exitProcess(0)
		}
		val diff = matMax - matMin
		(0 until field.length).forEachNested { x, y ->
		  if (mat[x][y] != null) {
			v[x][y] = ((mat[x][y]!! - matMin)/diff).toDouble()
		  } else {
			v[x][y] = Double.NaN
		  }

		}
	  }
	  ADD_POINT_5 -> {
		(0 until field.length).forEachNested { x, y ->        /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/        /*it seems to start at a=0.51*/        /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/

		  /*if (popCfg.conCircles) (concentricCircles) else mat*/

		  /*println("getting vis x=${x} y = ${y}")
		  println("size of mat is ${mat.size},${mat[0].size}")*/

		  if (mat[x][y] != null) {
			v[x][y] = /*max(min(*/mat[x][y]!!.toDouble()!!/* + 0.5, 1.0), 0.0)*/
		  } else {
			v[x][y] = Double.NaN
		  }

		}
	  }
	}
	v
  }

}

/*probably very inefficieent in at least one obvious way*/
class Mult(val f1: FieldGenerator, val f2: FieldGenerator): FieldGenerator(
  popCfg = f1.popCfg, field = f1.field, STIM_REC_CC_THETA_STEP = f1.STIM_REC_CC_THETA_STEP
) {

  override fun pix(p: Point): Float? {
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

interface CommonParams {
  val f: FieldLocI
}

data class Circle(val radius: Double, val center: Point = Point(x = ZERO, y = ZERO)) {
  operator fun contains(p: Point): Boolean {

	return center.normDist(p) <= radius
  }

}

data class ACircle(val radius: Apfloat, val center: APoint = APoint(x = 0.0.toApfloat(), y = 0.0.toApfloat())) {
  operator fun contains(p: APoint): Boolean {

	return center.normDist(p) <= radius
  }

  operator fun contains(p: Point): Boolean {

	return center.normDist(p) <= radius
  }

}

data class Stimulus(

  override val popCfg: PopulationConfig,

  override val f: FieldLocAndOrientation,


  val a: Double, /*ALPHA_CONTRAST*/
  val s: Double, /*SIGMA_SIZE*/
  override val SF: Double,

  val mask: Stimulus? = null,
  val gaussianEnveloped: Boolean = true,

  ): FieldGenerator(
  popCfg = popCfg, field = f.field, STIM_REC_CC_THETA_STEP = popCfg.sampleCCThetaStep
), Orientation by f, CommonParams, Subregioned {

  override val sx = s
  override val sy = s

  fun gaborParamString() =
	"alpha(contrast) = $a" + "\nSF = $SF" + "\ntheta = $tDegrees" + "\nsigma(size) = $sx($sy)" + "\nsample shape = ${(if (popCfg.conCircles) "concentric circles" else "rectangular matrix")}" + "\npoints sampled = ${(if (popCfg.conCircles) concentricCircles.size else mat.size)}" + "\nabsLimXY=${field.absLimXY}" + "\nsampleStep=${field.sampleStep}" + "\nX0_STEP=${popCfg.cellX0Step}" + "\nCELL_PREF_THETA_STEP=${popCfg.cellPrefThetaStep}" + "\nSTIM_REC_CC_THETA_STEP=${popCfg.sampleCCThetaStep}"

  private val sDen by lazy { 2.0*s.sq() }

  /*var debug: Boolean = false*/
  override fun pix(p: Point): Float? {

	/*println("getting stim px: ${p}")*/

	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) {
	  return null /*Double.NaN*/
	}


	val xyt = xyTheta(x = p.x, y = p.y)    /*val xT = xTheta(x = p.x, y = p.y)
	val yT = yTheta(x = p.x, y = p.y)*/


	val envelope = SigmaDenominator(s).let { sd ->
	  Envelope(
		gaussianEnveloped = gaussianEnveloped, xyt = xyt, sigmaX = sd, sigmaY = sd
	  )()
	}

	val phase = SamplePhase(xT = xyt, SF = SF, phase = COS)()
	val base = (a*envelope*phase)

	return base.let {
	  if (mask != null) it + mask.pix(p)!! else it
	}.toFloat()
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Orientation, Subregioned

enum class Phase {
  SIN, COS;

  fun comp(d: Double) = when (this) {
	SIN -> sin(d)
	COS -> cos(d)
  }
}

data class SimpleCell(
  override val popCfg: PopulationConfig,
  override val f: FieldLocAndOrientation,
  override val sx: Double, /*SIGMA_WIDTH*/
  override val sy: Double, /*SIGMA_HEIGHT*/
  override val SF: Double,
  val phase: Phase,
  val gaussianEnveloped: Boolean = true
): FieldGenerator(
  popCfg = popCfg, field = f.field, STIM_REC_CC_THETA_STEP = popCfg.sampleCCThetaStep
), Orientation by f, CommonParams, Cell {

  /*TODO: should combine with stimulus pix function*/
  override fun pix(p: Point): Float? {
	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) return null
	val xyt = xyTheta(x = p.x, y = p.y)
	return (Envelope(
	  gaussianEnveloped = gaussianEnveloped, xyt = xyt, sigmaX = SigmaDenominator(sx), sigmaY = SigmaDenominator(sy)
	)()*SamplePhase(
	  xT = xyt, SF = SF, phase = phase
	)()).toFloat()
  }


  fun stimulate(stim: Stimulus) = if (popCfg.conCircles) (stim.concentricCircles dot concentricCircles).also {    /*if (X0 == 0.0 && Y0 == 0.0 && t == 0.0 && stim.t == t) {
	  println("SF=${SF},phase=${phase},V=${it}")
	  println("size=${concentricCircles.size}")
	}*/
  }
  else run {    /*val t = tic(prefix = "stimulate simple cell")*/    /*t.sampleEveryByPrefix(100) {*/    //	  t.toc("start")
	val m1 = stim.mat    //	  t.toc("got 1")
	val flat1 = m1.flatten()    //	  t.toc("flattened 1")
	val m2 = mat    //	  t.toc("got 2")
	val flat2 = m2.flatten()    //	  t.toc("flattened 2")
	val dp = flat1 dot flat2    //	  t.toc("got dp")
	dp    /*}*/
  }
}


val DYNAMIC_BASELINE_B = 0.0.toApfloat()

/*val BASE_SIGMA_POOLING = *//*5.0*/
val ASD_SIGMA_POOLING = PopulationConfig().baseSigmaPooling*0.8.toApfloat()
val ATTENTION_SUPP_SIGMA_POOLING = PopulationConfig().baseSigmaPooling*1.5.toApfloat()

val ATTENTION_X = 0.0
val ATTENTION_Y = 0.0
val ATTENTION_CENTER = object: FieldLocI {
  override val X0 = ATTENTION_X
  override val Y0 = ATTENTION_Y
  override val field get() = NEVER

}

data class ComplexCell(
  val sinCell: SimpleCell, val cosCell: SimpleCell
): Cell by sinCell {
  constructor(cells: Pair<SimpleCell, SimpleCell>): this(cells.first, cells.second)

  init {
	require(sinCell.phase == SIN && cosCell.phase == COS)
  }

  var lastResponse = Response(R = 0.0, G_S = 0.0)

  fun stimulate(
	stim: Stimulus,
	uniformW: Apfloat?,
	rawInput: Apfloat?,
	attention: Boolean = false,
	popR: PopulationResponse? = null,
	ti: Apint? = null,
	h: Apfloat? = null,
  ): Response {    /*val t = tic(prefix = "stimulate complex cell")*/    //	return t.sampleEveryByPrefix(100) {
	//	  t.toc("start")
	var G: Double? = null
	if (ti == Apfloat.ZERO) return/*@sampleEveryByPrefix*/ Response(R = 0.0, G_S = G)    //	  t.toc("getting divNormS")
	val divNormS = if (popR == null) null else {
	  val realH = h ?: 1.0.toApfloat()
	  val S = getSfor(
		popR, sigmaPooling = popR.sigmaPooling, uniformW = uniformW?.toDouble()
	  )
	  if (ti == Apint.ZERO) 0.0.toApfloat() else {
		lastResponse.G_S!!.toApfloat() + realH*(-lastResponse.G_S!!.toApfloat() + S)
	  }
	}    /*val g1Back = if (divNormS != null && ti != null) divNormS else 0.0*/    //	  t.toc("getting input")
	val input = rawInput ?: run {    //		t.toc("getting input 1")
	  val sinR = sinCell.stimulate(stim)    //		t.toc("getting input 2")
	  val cosR = cosCell.stimulate(stim)    //		t.toc("getting input 3")
	  val inp = sinR.sq() + cosR.sq()    //		t.toc("getting input 4")
	  inp
	}    //	  t.toc("getting G")
	G = divNormS?.toDouble()    //	  t.toc("getting r")
	var r =
	  ((if (ti != null) DYNAMIC_BASELINE_B.toDouble() else sinCell.popCfg.DC_BASELINE_ACTIVITY) + input.toDouble()).let {
		if (popR == null) {
		  if (attention) {
			it*AttentionAmp(
			  norm = stim.normTo(ATTENTION_CENTER)
			).findOrCompute()
		  } else it
		} else if (ti == null) popR[this@ComplexCell]!!.R /*why... its the same right?*/ else it
	  }.let {
		val pw = popR?.divNorm?.copy(
		  D = it.toApfloat(),
		  S = divNormS,
		)?.findOrCompute() ?: it
		pw
	  }
	if (ti != null) {
	  val riLast = lastResponse.R
	  r = riLast.toApfloat() + (h!!*(-riLast.toApfloat() + r))
	}    //	  t.toc("getting Response")
	val resp = Response(
	  R = r.toDouble(), G_S = G
	)
	if (h != null) {
	  lastResponse = resp
	}    //	  t.toc("returning")
	return/*@sampleEveryByPrefix*/ resp    //	}
  }

  val weightMaps = mutableMapOf<Apfloat, MutableMap<ComplexCell, Double>>().withStoringDefault { sigmaPooling ->
	mutableMapOf<ComplexCell, Double>().withStoringDefault {
	  Weight(norm = normDistTo(it).toApfloat(), sigmaPool = sigmaPooling)()
	}
  }

  private fun getSfor(
	popActivity: Map<ComplexCell, Response>, sigmaPooling: Apfloat, uniformW: Double?
  ): Apfloat {
	return popActivity.entries.sumOf {
	  val contrib = ((uniformW ?: weightMaps[sigmaPooling][it.key]!!)*it.value.R /*W * r*/)
	  contrib.requireIsSafe()
	  contrib
	}.toApfloat()
  }
}

data class Response(
  val R: Double, val G_S: Double?
)


data class PopulationResponse(
  val m: Map<ComplexCell, Response>, val sigmaPooling: Apfloat, val divNorm: DivNorm
): Map<ComplexCell, Response> by m {
  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
}


