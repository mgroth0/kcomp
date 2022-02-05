@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.date.tic
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.e
import matt.kjlib.jmath.max
import matt.kjlib.jmath.min
import matt.kjlib.jmath.plus
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

/*just for visualizing*/
enum class NormMethod {
  RATIO, ADD_POINT_5
}

val NORM = ADD_POINT_5


data class Field(
  val absLimXY: Double,
  val stepXY: Double
) {
  val range = (-absLimXY..absLimXY step stepXY).toList()
  val length = range.size
  val circle = Circle(radius = absLimXY)

}

private var DEBUG_PRINTED = false


interface FieldLocI {


  val X0: Double /*HORIZONTAL_POS*/
  val Y0: Double /*VERTICAL_POS*/
  val field: Field /*HEIGHT_WIDTH*/

  /*TODO: better naming*/
  infix fun normTo(other: FieldLocI) = Norm(
	dX = X0 - other.X0, dY = Y0 - other.Y0
  )

  infix fun normDistTo(other: FieldLocI) = normTo(other).findOrCompute()
}

data class FieldLocAndOrientation(
  override val t: Double, override val X0: Double, override val Y0: Double, override val field: Field
): Orientation

interface Orientation: FieldLocI {
  val t: Double /*THETA_ORIENTATION*/
  val tRadians get() = Radians(t)()
  fun xTheta(x: Double, y: Double) = XTheta(t = tRadians, dX = x - X0, dY = y - Y0)()
  fun yTheta(x: Double, y: Double) = YTheta(t = tRadians, dX = x - X0, dY = y - Y0)()
}

private const val CACHE_SIZE = 10_000

val stimCache = LRUCache<Stimulus, NDArray<Double, D2>>(CACHE_SIZE)
val cellCache = LRUCache<Cell, NDArray<Double, D2>>(CACHE_SIZE)

val stimCacheCC = LRUCache<Stimulus, DoubleArray>(CACHE_SIZE)
val cellCacheCC = LRUCache<Cell, DoubleArray>(CACHE_SIZE)

abstract class FieldGenerator(
  private val field: Field,
  val X0_STEP: Double,
  val STIM_REC_CC_THETA_STEP: Double
) {

  protected abstract fun pix(p: Point): Double

  val mat by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCache[this]
	  is Cell     -> cellCache[this]
	  else        -> null
	}

	val m = cache ?: mk.empty<Double, D2>(field.length, field.length).let { m ->
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

	val m = cache ?: mutableListOf(pix(Point(x = 0.0, y = 0.0))).apply {
	  (X0_STEP..field.absLimXY step X0_STEP).forEach { r ->

		val normR = r*(1.0/X0_STEP)
		val circleThetaStep = (STIM_REC_CC_THETA_STEP)/normR

		var a = 0.0        /*if (this@FieldGenerator is Stimulus && X0 == 0.0 && Y0 == 0.0 && t in listOf(45.0, 90.0) && r ==1.0) {

		}*/
		while (a < 360.0) {
		  val p = pix(Polar(radius = r, rads = Radians(a))())
		  add(p)
		  a += circleThetaStep
		}
	  }
	}.toDoubleArray()

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


  fun getVisSample(x: Double, y: Double): Double {
	val sX = kotlin.math.min(kotlin.math.floor(x*vis.size).toInt(), vis.size - 1)
	val sY = kotlin.math.min(kotlin.math.floor(y*vis.size).toInt(), vis.size - 1)
	return vis[sX][sY]
  }

  private val vis by lazy {
	val v = (0 until field.length).map { DoubleArray(field.length) }.toTypedArray()
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
			v[x][y] = ((mat[x, y] - matMin)/diff).toDouble()
		  } else {
			v[x][y] = Double.NaN
		  }

		}
	  }
	  ADD_POINT_5 -> {
		(0 until field.length).forEachNested { x, y ->        /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/        /*it seems to start at a=0.51*/        /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/

		  if (!mat[x, y].isNaN()) {
			v[x][y] = max(min(mat[x][y].toApfloat() + 0.5.toApfloat(), 1.0.toApfloat()), 0.0.toApfloat()).toDouble()
		  } else {
			v[x][y] = Double.NaN
		  }

		}
	  }
	}
	v
  }

}


interface CommonParams {
  val f: FieldLocI
  val SF: Double /*SPATIAL_FREQ*/
}

data class Circle(val radius: Double, val center: Point = Point(x = 0.0, y = 0.0)) {
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

  val popCfg: PopulationConfig,

  override val f: FieldLocAndOrientation,


  val a: Double, /*ALPHA_CONTRAST*/
  val s: Double, /*SIGMA_SIZE*/
  override val SF: Double,

  val mask: Stimulus? = null,
  val gaussianEnveloped: Boolean = true,

  ): FieldGenerator(
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
	assert(this != 0.0)
  }


  override fun pix(p: Point): Double {
	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) {
	  return Double.NaN
	}


	return ((if (gaussianEnveloped) (a*(e.pow( /*TODO: alpha probably shouldn't be inside of "gaussianEnveloped"*/
	  -(xTheta(p.x, p.y).sq()/sDen) - (yTheta(
		p.x, p.y
	  ).sq()/sDen)
	))) else 1.0)*cos(
	  SF*xTheta(
		p.x, p.y
	  )
	)).let {
	  if (mask != null) it + mask.pix(p) else it
	}.toDouble()
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Orientation


data class SimpleCell(
  val popCfg: PopulationConfig,
  override val f: FieldLocAndOrientation, val sx: Double, /*SIGMA_WIDTH*/
  val sy: Double, /*SIGMA_HEIGHT*/
  override val SF: Double, val phase: Phase, val gaussianEnveloped: Boolean = true
): FieldGenerator(
  field = f.field,
  X0_STEP = popCfg.cellX0Step,
  STIM_REC_CC_THETA_STEP = popCfg.stimRecCCThetaStep
), Orientation by f, CommonParams, Cell {

  enum class Phase { SIN, COS }

  private fun phaseFun(d: Double) = when (phase) {
	SIN -> sin(d)
	COS -> cos(d)
  }

  private val sxDen = (2*sx.sq())
  private val syDen = (2*sy.sq())

  override fun pix(p: Point): Double {
	if (popCfg.matCircles && !popCfg.conCircles && p !in field.circle) return Double.NaN
	return ((if (gaussianEnveloped) e.pow(
	  -(xTheta(p.x, p.y).sq()/sxDen) - (yTheta(
		p.x, p.y
	  ).sq()/syDen)
	) else 1.0)*phaseFun(
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

val BASELINE_ACTIVITY = 2.0
val DC = BASELINE_ACTIVITY
val DYNAMIC_BASELINE_B = 0.0

val BASE_SIGMA_POOLING = /*5.0*/sqrt(5.0)
val ASD_SIGMA_POOLING = BASE_SIGMA_POOLING*0.8
val ATTENTION_SUPP_SIGMA_POOLING = BASE_SIGMA_POOLING*1.5

val ATTENTION_X = 0.0
val ATTENTION_Y = 0.0
val ATTENTION_CENTER = object: FieldLocI {
  override val X0 = ATTENTION_X
  override val Y0 = ATTENTION_Y
  override val field get() = NEVER

}

val tdDivNorm = DivNorm(
  D = null,
  c = 1.0*(10.0.pow(0))/*1.0*(10.0.pow(-4))*/,
  v = 1.0,
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

  var r1Back = 0.0

  /*var r2Back = 0.0*/
  var g1Back = 0.0
  /*var g2Back = 0.0*/
  /*val xiRiMap = mutableMapOf(0 to 0.0)
  val xiGiMap = mutableMapOf(0 to 0.0)*/


  fun stimulate(
	stim: Stimulus,
	uniformW: Double?,
	rawInput: Double?,
	attention: Boolean = false,
	popR: PopulationResponse? = null,
	ti: Int? = null,
	h: Double? = null,
  ): Pair<Double, Double?> {
	val t = tic(prefix = "stimulate", enabled = false)
	/*t.toc("getting G")*/
	var G: Double? = null
	if (ti == 0) return 0.0 to G
	/*val lastG = ti?.let { xiGiMap[it - 1] } ?: 0.0*/
	/*t.toc("getting divNormS")*/
	val divNormS = if (popR == null) null else {
	  /*t.toc("getting realH")*/
	  val realH = h ?: 1.0
	  /*t.toc("getting S")*/
	  val S = getSfor(
		popR, attention, sigmaPooling = popR.sigmaPooling, uniformW = uniformW
	  )
	  if (debugging) {
		println("ti=${ti}")
		println("realH=${realH}")
		println("lastG=${g1Back}")
		println("S=${S}")
	  }
	  /*t.toc("getting final divNormS")*/
	  if (ti == 0) 0.0 else {
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

  val weightMaps = mutableMapOf<Double, MutableMap<ComplexCell, Double>>().withStoringDefault { sigmaPooling ->
	mutableMapOf<ComplexCell, Double>().withStoringDefault {
	  Weight(norm = normDistTo(it), sigmaPool = sigmaPooling)()
	}
  }

  private fun getSfor(
	popActivity: Map<ComplexCell, Pair<Double, Double?>>,
	attentionExp: Boolean,
	sigmaPooling: Double,
	uniformW: Double?
  ): Double {

	/*	var i = 0*/


	return popActivity.entries.sumOf {
	  /*i++*/
	  //	  val t = tic(prefix = "getSfor(oneCell)", enabled = i == 100)
	  //	  t.toc("getting W")
	  /*println("cell=${it.key}")
	  println("pre=${it.value}")*/
	  /*val W = uniformW ?: weightMaps[sigmaPooling][it.key]!!*//* Weight(norm = normDistTo(it.key), sigmaPool = sigmaPooling)()*/

	  /*t.toc("getting W old way")*/
	  /*val oldW =
		uniformW ?: Weight(norm = normDistTo(it.key), sigmaPool = sigmaPooling)()*/

	  /*println("W=$W")*/
	  /*t.toc("getting r")*/
	  /*val r =*/ (uniformW ?: weightMaps[sigmaPooling][it.key]!!)*it.value.first /*W * r*/
	  /*println("contrib=$r")*/
	  /*t.toc("got r")*/
	  /*r*/
	}/*.sum()*/
  }
}


data class PopulationResponse(
  val m: Map<ComplexCell, Pair<Double, Double?>> = mapOf(),
  val sigmaPooling: Double = BASE_SIGMA_POOLING,
  val divNorm: DivNorm = tdDivNorm
): Map<ComplexCell, Pair<Double, Double?>> by m {
  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
}


