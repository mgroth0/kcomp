@file:Suppress("PropertyName")

package matt.v1.model

import matt.kjlib.jmath.e
import matt.kjlib.log.NEVER
import matt.kjlib.stream.forEachNested
import matt.klib.dmap.withStoringDefault
import matt.klib.ranges.step
import matt.v1.model.NormMethod.ADD_POINT_5
import matt.v1.model.NormMethod.RATIO
import matt.v1.model.SimpleCell.Phase.COS
import matt.v1.model.SimpleCell.Phase.SIN
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import org.jetbrains.kotlinx.multik.ndarray.operations.min
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
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
  val length = range.size.apply {
	println("field.length:${this}")
  }
}


interface FieldLocI {


  val X0: Double /*HORIZONTAL_POS*/
  val Y0: Double /*VERTICAL_POS*/
  val field: Field /*HEIGHT_WIDTH*/

  infix fun normDistTo(other: FieldLocI): Double {
	//	println("DEBUG normDistTo")
	//	tab("X0=$X0")
	//	tab("other.X0=${other.X0}")
	//	tab("Y0=${Y0}")
	//	tab("other.Y0=${other.Y0}")
	val r = Math.sqrt((X0 - other.X0).pow(2) + (Y0 - other.Y0).pow(2))
	//	tab("r=${r}")
	return r
  }

}

data class FieldLocAndOrientation(
  override val t: Double,
  override val X0: Double,
  override val Y0: Double,
  override val field: Field
): Orientation

interface Orientation: FieldLocI {
  val t: Double /*THETA_ORIENTATION*/
  val tRadians get() = Math.toRadians(t).apply { assert(!this.isNaN()) }
  fun xTheta(x: Double, y: Double) =
	  (cos(tRadians)*(x - X0) + sin(tRadians)*(y - Y0)).apply { assert(!this.isNaN()) }

  fun yTheta(x: Double, y: Double) =
	  (-sin(tRadians)*(x - X0) + cos(tRadians)*(y - Y0)).apply { assert(!this.isNaN()) }
}


abstract class FieldGenerator(
  private val field: Field
) {

  protected abstract fun pix(x: Double, y: Double): Double

  val mat by lazy {
	//	val m = field.range.map { DoubleArray(field.length) }.toTypedArray()
	val m = mk.empty<Double, D2>(field.length, field.length)
	//	globaltoc("generating matrix for $this")


	(0 until field.length).forEachNested { x, y ->
	  m[x, y] = pix(field.range[x], field.range[y]).apply { require(!this.isNaN()) }
	}
	//	globaltoc("generated matrix for $this")
	m
  }


  val min by lazy { mat.min()!! }
  val max by lazy { mat.max()!! }

  fun getVisSample(x: Double, y: Double): Double {
	val sX = min(floor(x*vis.size).toInt(), vis.size - 1)
	val sY = min(floor(y*vis.size).toInt(), vis.size - 1)
	//	println("x=${x},y=${y},Sx=${sX},sY=${sY}")
	return vis[sX][sY]
  }

  val vis by lazy {
	val v = (0 until field.length).map { DoubleArray(field.length) }.toTypedArray()
	when (NORM) {
	  RATIO       -> {
		if (min == max) {
		  println("stopping because min equals max")
		  exitProcess(0)
		}
		val diff = max - min
		(0 until field.length).forEachNested { x, y ->
		  v[x][y] = (mat[x, y] - min)/diff
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

data class Stimulus(
  override val f: FieldLocAndOrientation,
  val a: Double, /*ALPHA_CONTRAST*/
  val s: Double, /*SIGMA_SIZE*/
  override val SF: Double,
  val mask: Stimulus? = null
): FieldGenerator(f.field), Orientation by f, CommonParams {
  private val sDen = (2*s.pow(2)).apply { assert(this != 0.0) }

  override fun pix(x: Double, y: Double): Double {
	return (a*(e.pow(-(xTheta(x, y).pow(2)/sDen) - (yTheta(x, y).pow(2)/sDen)))*cos(
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
  val phase: Phase
): FieldGenerator(f.field), Orientation by f, CommonParams, Cell {

  enum class Phase { SIN, COS }

  private fun phaseFun(d: Double) = when (phase) {
	SIN -> sin(d)
	COS -> cos(d)
  }

  private val sxDen = (2*sx.pow(2))
  private val syDen = (2*sy.pow(2))

  override fun pix(x: Double, y: Double): Double {
	return ((e.pow(-(xTheta(x, y).pow(2)/sxDen) - (yTheta(x, y).pow(2)/syDen)))*phaseFun(
	  SF*xTheta(
		x,
		y
	  )
	)).apply { assert(!this.isNaN()) }
  }

  fun stimulate(stim: Stimulus): Double {
	//	globaltoc("running dot product")
	/*this is a different calculation...*/
	/*val d = mk.linalg.dot(stim.mat, mat).sum()*/

	var e = 0.0
	(0 until field.length).forEachNested { x, y ->
	  e += stim.mat[x][y]*mat[x][y]
	}
	//	println("finished running dot product: ${e}")
	return e
  }
}

const val BASELINE_ACTIVITY = 2.0
const val DC = BASELINE_ACTIVITY

val SIGMA_POOLING = Math.sqrt(5.0)
val S_POOL_SQUARED_DOUBLED = 2*SIGMA_POOLING.pow(2)

val SEMI_SATURATION_CONSTANT = 1.0
val v = SEMI_SATURATION_CONSTANT

val SUPPRESSIVE_FIELD_GAIN_TYPICAL = 1.0*10.0.pow(-4)
val SUPPRESSIVE_FIELD_GAIN_AUTISM = 7.5*10.0.pow(-5)

val ATTENTION_X = 0.0
val ATTENTION_Y = 0.0
val ATTENTION_CENTER = object: FieldLocI {
  override val X0 = ATTENTION_X
  override val Y0 = ATTENTION_Y
  override val field get() = NEVER

}
const val G = 7
val SIGMA_ATTENTION = 2.0

data class ComplexCell(val sinCell: SimpleCell, val cosCell: SimpleCell): Orientation by sinCell, Cell {
  constructor(cells: Pair<SimpleCell, SimpleCell>): this(cells.first, cells.second)

  init {
	require(sinCell.phase == SIN && cosCell.phase == COS)
  }


  fun stimulate(
	stim: Stimulus,
	divNormS: Double? = null,
	autism: Boolean = false,
	attention: Boolean = false,
	tempC: Double? = null
  ): Double {
	return (DC + sinCell.stimulate(stim).pow(2) + cosCell.stimulate(stim).pow(2)).let {
	  if (attention) {
		//		println("pre-attention:${it}")
		it*(1 + G*e.pow(-((stim.normDistTo(ATTENTION_CENTER).pow(2))/(2*SIGMA_ATTENTION.pow(2)))))
	  } else it
	}.let {
	  //	  if (attention) {
	  //		println("post-attention:${it}")
	  //	  }
	  if (divNormS != null) {
		val c = tempC ?: if (autism) SUPPRESSIVE_FIELD_GAIN_AUTISM else SUPPRESSIVE_FIELD_GAIN_TYPICAL
		(it/(v + c*divNormS))
	  } else it
	}
  }

  val weights = mutableMapOf<ComplexCell, Double>().withStoringDefault { other ->
	val v = e.pow(
	  (-(this.normDistTo(other)
		  .pow(2)))/(S_POOL_SQUARED_DOUBLED)
	)

	//	println("newW=${v}")
	v
  }


}