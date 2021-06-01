@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.cache.LRUCache
import matt.kjlib.jmath.dot
import matt.kjlib.jmath.e
import matt.kjlib.log.NEVER
import matt.kjlib.stream.forEachNested
import matt.klib.math.sq
import matt.klib.ranges.step
import matt.reflect.toStringBuilder
import matt.v1.compcache.AttentionAmp
import matt.v1.compcache.DivNorm
import matt.v1.compcache.Norm
import matt.v1.compcache.Point
import matt.v1.compcache.Weight
import matt.v1.compcache.XTheta
import matt.v1.compcache.YTheta
import matt.v1.lab.rcfg.rCfg
import matt.v1.model.NormMethod.ADD_POINT_5
import matt.v1.model.NormMethod.RATIO
import matt.v1.model.SimpleCell.Phase.COS
import matt.v1.model.SimpleCell.Phase.SIN
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.filter
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import org.jetbrains.kotlinx.multik.ndarray.operations.min
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
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
  val tRadians get() = Math.toRadians(t)
  fun xTheta(x: Double, y: Double) = XTheta(t = tRadians, dX = x - X0, dY = y - Y0)().also {
	//	println("xTheta t=$t x=$x y=$y result=$it")
  }

  fun yTheta(x: Double, y: Double) = YTheta(t = tRadians, dX = x - X0, dY = y - Y0)()
}

private const val CACHE_SIZE = 10_000

val stimCache = LRUCache<Stimulus, MultiArray<Double, D2>>(CACHE_SIZE)
val cellCache = LRUCache<Cell, MultiArray<Double, D2>>(CACHE_SIZE)

val stimCacheCC = LRUCache<Stimulus, DoubleArray>(CACHE_SIZE)
val cellCacheCC = LRUCache<Cell, DoubleArray>(CACHE_SIZE)

abstract class FieldGenerator(
  private val field: Field,
) {

  protected abstract fun pix(x: Double, y: Double): Double

  val mat by lazy {
	val cache = when (this) {
	  is Stimulus -> stimCache[this]
	  is Cell     -> cellCache[this]
	  else        -> null
	}

	val m = cache ?: mk.empty<Double, D2>(field.length, field.length).let { m ->
	  (0 until field.length).forEachNested { x, y ->
		m[x, y] = pix(field.range[x], field.range[y])
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

	val m = cache ?: mutableListOf(pix(x = 0.0, y = 0.0)).apply {
	  (1 until floor(field.absLimXY).roundToInt()).forEach { r ->
		val circleThetaStep = (rCfg.CELL_THETA_STEP*rCfg.DEBUG_3D_THETA_MULT)/r
		var a = 0.0
		while (a < 360.0) {
		  val x = r*cos(Math.toRadians(a))
		  val y = r*sin(Math.toRadians(a))
		  val p = pix(x = x, y = y)
		  if (r == 1) {
			//			println("x=$x,y=$y,r=$r,a=$a,p=$p")
		  }
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
	m
  }


  fun getVisSample(x: Double, y: Double): Double {
	val sX = min(floor(x*vis.size).toInt(), vis.size - 1)
	val sY = min(floor(y*vis.size).toInt(), vis.size - 1)
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
		  v[x][y] = (mat[x, y] - matMin)/diff
		}
	  }
	  ADD_POINT_5 -> {
		(0 until field.length).forEachNested { x, y ->
		  /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/
		  /*it seems to start at a=0.51*/
		  /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/
		  v[x][y] = max(min(mat[x][y] + 0.5, 1.0), 0.0)
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


  override fun pix(x: Double, y: Double): Double {
	if (Point(x = x, y = y) !in field.circle) {
	  /*println("getting nan for x=$x y=$y")*/
	  return Double.NaN
	}

	/*warnOnce("debugging stim pix")
	return xTheta(
	  x,
	  y
	)*/

	return ((if (gaussianEnveloped) (a*(e.pow( /*TODO: alpha probably shouldn't be inside of "gaussianEnveloped"*/
	  -(xTheta(x, y).sq()/sDen) - (yTheta(
		x,
		y
	  ).sq()/sDen)
	))) else 1.0)*cos(
	  SF*xTheta(
		x,
		y
	  )
	)).let {
	  if (mask != null) it + mask.pix(x, y) else it
	}.apply {
	  assert(!this.isNaN()) {
		"a=$a,e=$e,xTheta=${xTheta(x, y)},yTheta=${yTheta(x, y)}sDen=$sDen,SF=$SF"
	  }
	}
  }

  infix fun withMask(other: Stimulus) = copy(mask = other)


}

interface Cell: Orientation {
  //  fun stimulate(stim: Stimulus): Double
}


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

  override fun pix(x: Double, y: Double): Double {
	if (Point(x = x, y = y) !in field.circle) return Double.NaN
	return ((if (gaussianEnveloped) e.pow(
	  -(xTheta(x, y).sq()/sxDen) - (yTheta(
		x,
		y
	  ).sq()/syDen)
	) else 1.0)*phaseFun(
	  SF*xTheta(
		x,
		y
	  )
	)).apply { assert(!this.isNaN()) }
  }


  fun stimulate(stim: Stimulus) =
	  stim.also {
		/*warn("debugging dot product")
		println("stim:$it")*/
	  }
		  /*.mat*/
		  .concentricCircles

		  .also {
			it.forEach {
			  /*println("cc:$it")*/
			}
			/*println("stim:${it.joinToString { it.toString() }}")*/
			/*println("stim.mat.sum=${it.filterNot { it.isNaN() }.sum()}")
			println("stim.mat.min=${it.filterNot { it.isNaN() }.minOrNull()!!}")
			println("stim.mat.max=${it.filterNot { it.isNaN() }.maxOrNull()!!}")
			println("stim.mat.mean=${it.filterNot { it.isNaN() }.toList().mean()}")*/
		  } dot concentricCircles

  /*mat*/ /*DoubleArray(stim.concentricCircles.size) { 1.0 }*/
  /*mk.ndarray(
		  DoubleArray(mat.size) { 1.0 },
		  mat.shape[0],
		  mat.shape[1]
		)*/
}

const val BASELINE_ACTIVITY = 2.0
const val DC = BASELINE_ACTIVITY

val BASE_SIGMA_POOLING = sqrt(5.0)
val ASD_SIGMA_POOLING = BASE_SIGMA_POOLING*0.8
val ATTENTION_SUPP_SIGMA_POOLING = BASE_SIGMA_POOLING*1.5

const val ATTENTION_X = 0.0
const val ATTENTION_Y = 0.0
val ATTENTION_CENTER = object: FieldLocI {
  override val X0 = ATTENTION_X
  override val Y0 = ATTENTION_Y
  override val field get() = NEVER

}

val tdDivNorm = DivNorm(
  D = null,
  c = 1.0*10.0.pow(-4),
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


  fun stimulate(
	stim: Stimulus,
	attention: Boolean = false,
	popR: PopulationResponse? = null,
  ): Double {
	//	println("stimulating $this")
	val divNormS =
		if (popR != null) getSfor(popR, attention, sigmaPooling = popR.sigmaPooling) else null
	val debug =
		(DC + sinCell.stimulate(stim).also { /*println("sin:${it}")*/ }.sq() + cosCell
			.stimulate(stim)
			.also { /*println("cos:${it}")*/ }
			.sq()).let {
		  if (popR == null) {
			if (attention) {
			  it*AttentionAmp(
				norm = stim.normTo(ATTENTION_CENTER)
			  ).findOrCompute()
			} else it
		  } else popR[this]!!
		}.let {
		  popR?.divNorm?.copy(
			D = it,
			S = divNormS,
		  )?.findOrCompute(debug = false) ?: it
		}
	//	println("r=${debug}")
	return debug/*/ *//*DEBUG*//*10.0*/
  }

  private fun getSfor(
	popActivity: Map<ComplexCell, Double>,
	attentionExp: Boolean,
	sigmaPooling: Double
  ): Double {
	val debug = if (attentionExp) 250 else 1

	return /*S=*/ (popActivity.map {
	  /*W=*/(Weight(norm = normDistTo(it.key/*c*/), sigmaPool = sigmaPooling).findOrCompute() /*W*/)*(it.value /*D*/)
	}.sum()*(/*DIRTY_S_MUlT*/rCfg.REQ_SIZE.toDouble()/popActivity.size.toDouble())*debug)
  }
}


data class PopulationResponse(
  val m: Map<ComplexCell, Double> = mapOf(),
  val sigmaPooling: Double = BASE_SIGMA_POOLING,
  val divNorm: DivNorm = tdDivNorm
): Map<ComplexCell, Double> by m {
  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
}


