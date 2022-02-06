@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.date.tic
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.eFloat
import matt.kjlib.jmath.max
import matt.kjlib.jmath.min
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.sumOf
import matt.kjlib.jmath.toApfloat
import matt.kjlib.log.NEVER
import matt.kjlib.ranges.step
import matt.kjlib.stream.forEachNested
import matt.klib.dmap.withStoringDefault
import matt.klib.math.sq
import matt.reflect.toStringBuilder
import matt.v1.compcache.APoint
import matt.v1.compcache.AttentionAmp
import matt.v1.compcache.DivNorm
import matt.v1.compcache.Norm
import matt.v1.compcache.Point
import matt.v1.compcache.Polar
import matt.v1.compcache.Radians
import matt.v1.compcache.Weight
import matt.v1.compcache.XTheta
import matt.v1.compcache.YTheta
import matt.v1.lab.petri.PopulationConfig
import matt.v1.model.NormMethod.ADD_POINT_5
import matt.v1.model.NormMethod.RATIO
import matt.v1.model.SimpleCell.Phase.COS
import matt.v1.model.SimpleCell.Phase.SIN
import org.apfloat.Apfloat
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.filter
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import org.jetbrains.kotlinx.multik.ndarray.operations.min
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.system.exitProcess

val ZERO = 0.0.toFloat()
val ONE = 1.0.toFloat()

/*just for visualizing*/
enum class NormMethod {
  RATIO, ADD_POINT_5
}

val NORM = ADD_POINT_5


data class Field(
  val absLimXY: Float,
  val stepXY: Float
) {
  val range = (-absLimXY..absLimXY step stepXY).toList()
  val length = range.size
  val circle = Circle(radius = absLimXY)
  val visCircle = Circle(radius = absLimXY, Point(x = absLimXY/2, y = absLimXY/2))

}

private var DEBUG_PRINTED = false


interface FieldLocI {


  val X0: Float /*HORIZONTAL_POS*/
  val Y0: Float /*VERTICAL_POS*/
  val field: Field /*HEIGHT_WIDTH*/

  /*TODO: better naming*/
  infix fun normTo(other: FieldLocI) = Norm(
	dX = X0 - other.X0, dY = Y0 - other.Y0
  )

  infix fun normDistTo(other: FieldLocI) = normTo(other).findOrCompute()
}

data class FieldLocAndOrientation(
  override val t: Float, override val X0: Float, override val Y0: Float, override val field: Field
): Orientation

interface Orientation: FieldLocI {
  val t: Float /*THETA_ORIENTATION*/
  val tRadians get() = Radians(t)()
  fun xTheta(x: Float, y: Float) = XTheta(t = tRadians, dX = x - X0, dY = y - Y0)()
  fun yTheta(x: Float, y: Float) = YTheta(t = tRadians, dX = x - X0, dY = y - Y0)()
}

private const val CACHE_SIZE = 10_000

val stimCache = LRUCache<Stimulus, NDArray<Float, D2>>(CACHE_SIZE)
val cellCache = LRUCache<Cell, NDArray<Float, D2>>(CACHE_SIZE)

val stimCacheCC = LRUCache<Stimulus, FloatArray>(CACHE_SIZE)
val cellCacheCC = LRUCache<Cell, FloatArray>(CACHE_SIZE)

abstract class FieldGenerator(
  open val popCfg: PopulationConfig,
  private val field: Field,
  val X0_STEP: Float,
  val STIM_REC_CC_THETA_STEP: Float
) {

  protected abstract fun pix(p: Point): Float

  val mat by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCache[this]
	  is Cell     -> cellCache[this]
	  else        -> null
	}

	val m = cache ?: mk.empty<Float, D2>(field.length, field.length).let { m ->
	  (0 until field.length).forEachNested { x, y ->
		m[x, y] = pix(Point(x = field.range[x], y = field.range[y]))
	  }
	  m
	}

	if (cache == null) {
	  when (this) {
		is Stimulus -> stimCache[this] = m
		is Cell     -> cellCache[this] = m
	  }
	}
	m
  }

  val concentricCircles by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCacheCC[this]
	  is Cell     -> cellCacheCC[this]
	  else        -> null
	}

	val m = cache ?: mutableListOf(pix(Point(x = ZERO, y = ZERO))).apply {
	  (X0_STEP..field.absLimXY step X0_STEP).forEach { r ->

		val normR = r*(ONE/X0_STEP)
		val circleThetaStep = (STIM_REC_CC_THETA_STEP)/normR

		var a = ZERO        /*if (this@FieldGenerator is Stimulus && X0 == 0.0 && Y0 == 0.0 && t in listOf(45.0, 90.0) && r ==1.0) {

		}*/
		while (a < 360.0) {
		  val p = pix(Polar(radius = r, rads = Radians(a))())
		  add(p)
		  a += circleThetaStep
		}
	  }
	}.toFloatArray()

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


  /*x y here are percentages like 0.25 might come in and that means 25%*/
  fun getVisSample(x: Float, y: Float): Float {

	if (popCfg.matCircles) {
	  val absX = x*field.absLimXY*2 - field.absLimXY
	  val absY = y*field.absLimXY*2 - field.absLimXY
	  if (Point(x = absX, y = absY) !in field.circle) return Float.NaN
	}


	val sX = kotlin.math.min(kotlin.math.floor(x*vis.size).toInt(), vis.size - 1)
	val sY = kotlin.math.min(kotlin.math.floor(y*vis.size).toInt(), vis.size - 1)

	return vis[sX][sY]
  }

  private val vis by lazy {
	val v = (0 until field.length).map { FloatArray(field.length) }.toTypedArray()
	when (NORM) {
	  RATIO       -> {
		val matMin by lazy { mat.filter { !it.isNaN() }.min()!! }
		val matMax by lazy { mat.filter { !it.isNaN() }.max()!! }
		if (matMin == matMax) {
		  println("stopping because min equals max")
		  exitProcess(0)
		}
		val diff = matMax - matMin
		(0 until field.length).forEachNested { x, y ->
		  if (!mat[x, y].isNaN()) {
			v[x][y] = ((mat[x, y] - matMin)/diff).toFloat()
		  } else {
			v[x][y] = Float.NaN
		  }

		}
	  }
	  ADD_POINT_5 -> {
		(0 until field.length).forEachNested { x, y ->        /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/        /*it seems to start at a=0.51*/        /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/
		  if (popCfg.conCircles) (concentricCircles) else mat
		  if (!mat[x, y].isNaN()) {
			v[x][y] = max(min(mat[x][y].toApfloat() + 0.5.toApfloat(), 1.0.toApfloat()), 0.0.toApfloat()).toFloat()
		  } else {
			v[x][y] = Float.NaN
		  }

		}
	  }
	}
	v
  }

}


interface CommonParams {
  val f: FieldLocI
  val SF: Float /*SPATIAL_FREQ*/
}

data class Circle(val radius: Float, val center: Point = Point(x = ZERO, y = ZERO)) {
  operator fun contains(p: Point): Boolean {

	return center.normDist(p) <= radius
  }

}

data class ACircle(val radius: Apfloat, val center: APoint = APoint(x = 0.0.toApfloat(), y = 0.0.toApfloat())) {
  operator fun contains(p: APoint): Boolean {

	return center.normDist(p) <= radius
  }

}

data class Stimulus(

  override val popCfg: PopulationConfig,

  override val f: FieldLocAndOrientation,


  val a: Float, /*ALPHA_CONTRAST*/
  val s: Float, /*SIGMA_SIZE*/
  override val SF: Float,

  val mask: Stimulus? = null,
  val gaussianEnveloped: Boolean = true,

  ): FieldGenerator(
  popCfg = popCfg,
  field = f.field,
  X0_STEP = popCfg.cellX0Step,
  STIM_REC_CC_THETA_STEP = popCfg.stimRecCCThetaStep
), Orientation by f, CommonParams {

  fun gaborParamString() =
	"alpha(contrast) = $a" +
		"\nSF = $SF" +
		"\ntheta = $t" +
		"\nsigma(size) = $s" +
		"\nsample shape = ${(if (popCfg.conCircles) "concentric circles" else "rectangular matrix")}" +
		"\npoints sampled = ${(if (popCfg.conCircles) concentricCircles.size else mat.size)}" +
		"\nabsLimXY=${field.absLimXY}" +
		"\nstepXY=${field.stepXY}" +
		"\nX0_STEP=${popCfg.cellX0Step}" +
		"\nCELL_PREF_THETA_STEP=${popCfg.cellPrefThetaStep}" +
		"\nSTIM_REC_CC_THETA_STEP=${popCfg.stimRecCCThetaStep}"

  private val sDen = (2*s.sq()).apply {
	assert(this != 0.0f)
  }


  override fun pix(p: Point): Float {
	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) {
	  return Float.NaN
	}


	return ((if (gaussianEnveloped) (a*(eFloat.pow( /*TODO: alpha probably shouldn't be inside of "gaussianEnveloped"*/
	  -(xTheta(p.x, p.y).sq()/sDen) - (yTheta(
		p.x, p.y
	  ).sq()/sDen)
	))) else 1.0f)*cos(
	  SF*xTheta(
		p.x, p.y
	  )
	)).let {
	  if (mask != null) it + mask.pix(p) else it
	}.toFloat()
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Orientation


data class SimpleCell(
  override val popCfg: PopulationConfig,
  override val f: FieldLocAndOrientation, val sx: Float, /*SIGMA_WIDTH*/
  val sy: Float, /*SIGMA_HEIGHT*/
  override val SF: Float, val phase: Phase, val gaussianEnveloped: Boolean = true
): FieldGenerator(
  popCfg = popCfg,
  field = f.field,
  X0_STEP = popCfg.cellX0Step,
  STIM_REC_CC_THETA_STEP = popCfg.stimRecCCThetaStep
), Orientation by f, CommonParams, Cell {

  enum class Phase { SIN, COS }

  private fun phaseFun(d: Float) = when (phase) {
	SIN -> sin(d)
	COS -> cos(d)
  }

  private val sxDen = (2*sx.sq())
  private val syDen = (2*sy.sq())

  override fun pix(p: Point): Float {
	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) return Float.NaN
	return ((if (gaussianEnveloped) eFloat.pow(
	  -(xTheta(p.x, p.y).sq()/sxDen) - (yTheta(
		p.x, p.y
	  ).sq()/syDen)
	) else 1.0f)*phaseFun(
	  SF*xTheta(
		p.x, p.y
	  )
	))
  }


  fun stimulate(stim: Stimulus) = if (popCfg.conCircles) (stim.concentricCircles dot concentricCircles)/*.also {
	  if (phase == SIN && X0 == 0.0 && Y0 == 0.0 && t in listOf(45.0, 90.0) && stim.t == t) {
		println("t=$t concentricCircles.sum=${concentricCircles.sum()} stim.concentricCircles.sum=${stim.concentricCircles.sum()}")
		taball("concentricCircles", concentricCircles)
	  }
	}*/
  else stim.mat dot mat
}

val BASELINE_ACTIVITY = 2.0f
val DC = BASELINE_ACTIVITY
val DYNAMIC_BASELINE_B = 0.0f

val BASE_SIGMA_POOLING = /*5.0*/sqrt(5.0f)
val ASD_SIGMA_POOLING = BASE_SIGMA_POOLING*0.8f
val ATTENTION_SUPP_SIGMA_POOLING = BASE_SIGMA_POOLING*1.5f

val ATTENTION_X = 0.0f
val ATTENTION_Y = 0.0f
val ATTENTION_CENTER = object: FieldLocI {
  override val X0 = ATTENTION_X
  override val Y0 = ATTENTION_Y
  override val field get() = NEVER

}

val tdDivNorm = DivNorm(
  D = null,
  c = 1.0f*(10.0f.pow(0))/*1.0*(10.0.pow(-4))*/,
  v = 1.0f,
  S = null
)

data class ComplexCell(
  val sinCell: SimpleCell, val cosCell: SimpleCell
): Orientation by sinCell, Cell {
  constructor(cells: Pair<SimpleCell, SimpleCell>): this(cells.first, cells.second)

  var debugging = false

  init {
	require(sinCell.phase == SIN && cosCell.phase == COS)
  }

  var r1Back = 0.0.toFloat()

  /*var r2Back = 0.0*/
  var g1Back = 0.0.toFloat()
  /*var g2Back = 0.0*/
  /*val xiRiMap = mutableMapOf(0 to 0.0)
  val xiGiMap = mutableMapOf(0 to 0.0)*/


  fun stimulate(
	stim: Stimulus,
	uniformW: Float?,
	rawInput: Float?,
	attention: Boolean = false,
	popR: PopulationResponse? = null,
	ti: Int? = null,
	h: Float? = null,
  ): Pair<Float, Float?> {
	val t = tic(prefix = "stimulate", enabled = false)
	/*t.toc("getting G")*/
	var G: Float? = null
	if (ti == 0) return 0.0.toFloat() to G
	/*val lastG = ti?.let { xiGiMap[it - 1] } ?: 0.0*/
	/*t.toc("getting divNormS")*/
	val divNormS = if (popR == null) null else {
	  /*t.toc("getting realH")*/
	  val realH = h ?: 1.0.toFloat()
	  /*t.toc("getting S")*/
	  val S = getSfor(
		popR, sigmaPooling = popR.sigmaPooling, uniformW = uniformW
	  )
	  if (debugging) {
		println("ti=${ti}")
		println("realH=${realH}")
		println("lastG=${g1Back}")
		println("S=${S}")
	  }
	  /*t.toc("getting final divNormS")*/
	  if (ti == 0) 0.0.toFloat() else {
		g1Back + realH*(-g1Back + S)
	  }
	}
	/*t.toc("updating xiGiMap")*/
	if (divNormS != null && ti != null) g1Back = divNormS

	G = divNormS

	/*t.toc("getting input")*/
	val input = rawInput ?: (sinCell.stimulate(stim).sq() + cosCell.stimulate(stim).sq())

	/*t.toc("getting debug")*/
	var debug = ((if (ti != null) DYNAMIC_BASELINE_B else DC) + input).let {
	  if (popR == null) {
		if (attention) {
		  it*AttentionAmp(
			norm = stim.normTo(ATTENTION_CENTER)
		  ).findOrCompute()
		} else it
	  } else if (ti == null) popR[this]!!.first /*why... its the same right?*/ else it
	}.let {
	  /*t.toc("computing divNorm")*//*println("debug5:${it}")*/
	  popR?.divNorm?.copy(
		D = it,
		S = divNormS,
	  )?.findOrCompute(debug = false) ?: it
	}

	/*t.toc("doing euler")*//*println("debug5:${it}")*/
	if (ti != null) {
	  val riLast = r1Back
	  debug = riLast + (h!!*(-riLast + debug))
	  r1Back = debug
	}

	/*t.toc("returning")*/
	return debug to G  /*/ *//*DEBUG*//*10.0*/
  }

  val weightMaps = mutableMapOf<Float, MutableMap<ComplexCell, Float>>().withStoringDefault { sigmaPooling ->
	mutableMapOf<ComplexCell, Float>().withStoringDefault {
	  Weight(norm = normDistTo(it), sigmaPool = sigmaPooling)()
	}
  }

  private fun getSfor(
	popActivity: Map<ComplexCell, Pair<Float, Float?>>,
	sigmaPooling: Float,
	uniformW: Float?
  ): Float {
	/*val t = tic(prefix = "getSfor")
	t.toc("getting products")
	val suppression = popActivity.entries.map {
	  t.sampleEvery(100) {
		*//*toc("getting W")*//*
		*//*1.1 to 1.2 µs*//**//* val W = (uniformW ?: weightMaps[sigmaPooling][it.key]!!)*//*
		*//*toc("getting R")*//*
		*//*1.9 to 1.05 µs*//**//* val R = it.value.first*//*
		toc("getting product")
		*//*0.95 to 1.15 µs*//**//* val product = W*R*//*
		*//*1 to 2.5 µs*//* val product = (uniformW ?: weightMaps[sigmaPooling][it.key]!!)*it.value.first
		toc("got product")
		product
	  }
	}
	t.toc("getting sum")
	*//*1.5 to 3.5 µs*//* val theSum = suppression.sum()
	t.toc("got sum")
	return theSum*/


	return popActivity.entries.sumOf {
	  (uniformW ?: weightMaps[sigmaPooling][it.key]!!)*it.value.first /*W * r*/
	}

	/*val m1 = mk.empty<Float, D1>(popActivity.entries.size)
	val m2 = mk.empty<Float, D1>(popActivity.entries.size)
	val weightMap = if (uniformW == null) weightMaps[sigmaPooling] else null
	popActivity.entries.forEachIndexed { index, entry ->
	  m1[index] = weightMap?.get(entry.key) ?: uniformW!!
	  m2[index] = entry.value.first
	}

	return m1.times(m2).sum()*/

  }
}


data class PopulationResponse(
  val m: Map<ComplexCell, Pair<Float, Float?>> = mapOf(),
  val sigmaPooling: Float = BASE_SIGMA_POOLING,
  val divNorm: DivNorm = tdDivNorm
): Map<ComplexCell, Pair<Float, Float?>> by m {
  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
}


