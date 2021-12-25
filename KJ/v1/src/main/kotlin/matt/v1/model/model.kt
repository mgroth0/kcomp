@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.e
import matt.kjlib.jmath.max
import matt.kjlib.jmath.min
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.toApfloat
import matt.kjlib.log.NEVER
import matt.kjlib.ranges.step
import matt.kjlib.stream.forEachNested
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
import matt.v1.lab.petri.pop2D
import matt.v1.lab.rcfg.rCfg
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
	dX = X0 - other.X0,
	dY = Y0 - other.Y0
  )

  infix fun normDistTo(other: FieldLocI) = normTo(other).findOrCompute()
}

data class FieldLocAndOrientation(
  override val t: Double,
  override val X0: Double,
  override val Y0: Double,
  override val field: Field
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
	  (rCfg.X0_STEP..field.absLimXY step rCfg.X0_STEP).forEach { r ->

		val normR = r*(1.0/rCfg.X0_STEP)
		val circleThetaStep = (rCfg.CELL_THETA_STEP)/normR

		var a = 0.0
		/*if (this@FieldGenerator is Stimulus && X0 == 0.0 && Y0 == 0.0 && t in listOf(45.0, 90.0) && r ==1.0) {

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
		(0 until field.length).forEachNested { x, y ->
		  /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/
		  /*it seems to start at a=0.51*/
		  /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/

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
  override val f: FieldLocAndOrientation,
  val a: Double, /*ALPHA_CONTRAST*/
  val s: Double, /*SIGMA_SIZE*/
  override val SF: Double,
  val mask: Stimulus? = null,
  val gaussianEnveloped: Boolean = true
): FieldGenerator(f.field), Orientation by f, CommonParams {
  private val sDen = (2*s.sq()).apply {
	assert(this != 0.0)
  }


  override fun pix(p: Point): Double {
	if (rCfg.MAT_CIRCLES && !rCfg.CON_CIRCLES && p !in field.circle) {
	  return Double.NaN
	}


	return ((if (gaussianEnveloped) (a*(e.pow( /*TODO: alpha probably shouldn't be inside of "gaussianEnveloped"*/
	  -(xTheta(p.x, p.y).sq()/sDen) - (yTheta(
		p.x,
		p.y
	  ).sq()/sDen)
	))) else 1.0)*cos(
	  SF*xTheta(
		p.x,
		p.y
	  )
	)).let {
	  if (mask != null) it + mask.pix(p) else it
	}.toDouble()
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Orientation


data class SimpleCell(
  override val f: FieldLocAndOrientation,
  val sx: Double, /*SIGMA_WIDTH*/
  val sy: Double, /*SIGMA_HEIGHT*/
  override val SF: Double,
  val phase: Phase,
  val gaussianEnveloped: Boolean = true
): FieldGenerator(f.field), Orientation by f, CommonParams, Cell {

  enum class Phase { SIN, COS }

  private fun phaseFun(d: Double) = when (phase) {
	SIN -> sin(d)
	COS -> cos(d)
  }

  private val sxDen = (2*sx.sq())
  private val syDen = (2*sy.sq())

  override fun pix(p: Point): Double {
	if (rCfg.MAT_CIRCLES && !rCfg.CON_CIRCLES && p !in field.circle) return Double.NaN
	return ((if (gaussianEnveloped) e.pow(
	  -(xTheta(p.x, p.y).sq()/sxDen) - (yTheta(
		p.x,
		p.y
	  ).sq()/syDen)
	) else 1.0)*phaseFun(
	  SF*xTheta(
		p.x,
		p.y
	  )
	))
  }


  fun stimulate(stim: Stimulus) = if (rCfg.CON_CIRCLES)
	(stim.concentricCircles dot concentricCircles)/*.also {
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
  val sinCell: SimpleCell,
  val cosCell: SimpleCell
): Orientation by sinCell, Cell {
  constructor(cells: Pair<SimpleCell, SimpleCell>): this(cells.first, cells.second)

  init {
	require(sinCell.phase == SIN && cosCell.phase == COS)
  }

  val xiRiMap = mutableMapOf(0 to 0.0)
  val xiGiMap = mutableMapOf(0 to 0.0)


  fun stimulate(
	stim: Stimulus,
	attention: Boolean = false,
	popR: PopulationResponse? = null,
	ti: Int? = null,
	h: Double? = null
  ): Double {
	if (ti == 0) return 0.0
	val divNormS =
	  if (popR != null) (if (ti == 0) 0.0 else (ti?.let { xiGiMap[it - 1] } ?: 0.0) + ((h
		?: 1.0)*(-(ti?.let { xiGiMap[it - 1] } ?: 0.0)) + getSfor(
		popR,
		attention,
		sigmaPooling = popR.sigmaPooling
	  ))) else null
	if (divNormS != null && ti != null) xiGiMap[ti - 1] = divNormS
	var debug =
	  ((if (ti != null) DYNAMIC_BASELINE_B else DC) + sinCell.stimulate(stim).sq()
		/*.also { println("debug1:${it}") }*/ + cosCell
		.stimulate(stim)/*.also { println("debug2:${it}") }*/
		.sq()/*.also { println("debug3:${it}") }*/).let {
//		println("debug4:${it} popR=${popR} attention=${attention} ti=${ti}")
		if (popR == null) {
		  if (attention) {
			it*AttentionAmp(
			  norm = stim.normTo(ATTENTION_CENTER)
			).findOrCompute()
		  } else it
		} else if (ti == null) popR[this]!! /*why... its the same right?*/ else it
	  }.let {
		/*println("debug5:${it}")*/
		popR?.divNorm?.copy(
		  D = it,
		  S = divNormS,
		)?.findOrCompute(debug = false) ?: it
	  }

	/*if (this == pop2D.complexCells[0]) {
	  println("debug=${debug}")
	  println("divNormS=${divNormS}")
	}*/

	if (ti != null) {
	  val riLast = xiRiMap[ti - 1]!!
	  debug = riLast + (h!!*(-riLast + debug))
	  xiRiMap[ti] = debug
	}

	return debug  /*/ *//*DEBUG*//*10.0*/
  }

  private fun getSfor(
	popActivity: Map<ComplexCell, Double>,
	attentionExp: Boolean,
	sigmaPooling: Double
  ): Double {

	return popActivity.map {
	  //	  println("cell=${it.key}")
	  //	  println("pre=${it.value}")
	  val W = Weight(norm = normDistTo(it.key), sigmaPool = sigmaPooling)()
	  //	  println("W=$W")
	  /*val r = */W*it.value
	  //	  println("contrib=$r")
	  /*r*/
	}.sum()
  }
}


data class PopulationResponse(
  val m: Map<ComplexCell, Double> = mapOf(),
  val sigmaPooling: Double = BASE_SIGMA_POOLING,
  val divNorm: DivNorm = tdDivNorm
): Map<ComplexCell, Double> by m {
  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
}


