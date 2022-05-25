@file:UseSerializers(ApfloatSerializer::class, ApintSerializer::class)

package matt.v1.low

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import matt.kbuild.REGISTERED_FOLDER
//import matt.kjlib.commons.REGISTERED_FOLDER
import matt.kjlib.compcache.ComputeCache
import matt.kjlib.compcache.ComputeInput
import matt.kjlib.file.filterHasExtension
import matt.kjlib.file.get
import matt.kjlib.file.recursiveChildren
import matt.kjlib.file.text
import matt.kjlib.jmath.API
import matt.kjlib.jmath.Ae
import matt.kjlib.jmath.PI
import matt.kjlib.jmath.cos
import matt.kjlib.jmath.div
import matt.kjlib.jmath.e
import matt.kjlib.jmath.logFactorial
import matt.kjlib.jmath.minus
import matt.kjlib.jmath.point.APoint
import matt.kjlib.jmath.point.BasicPoint
import matt.kjlib.jmath.point.Point
import matt.kjlib.jmath.pow
import matt.kjlib.jmath.ser.ApfloatSerializer
import matt.kjlib.jmath.ser.ApintSerializer
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.jmath.unaryMinus
import matt.kjlib.ranges.step
import matt.klib.log.warn
import matt.klib.math.sq
import org.apache.commons.math3.exception.ConvergenceException
import org.apache.commons.math3.fitting.GaussianCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import org.apfloat.Apint
import org.apfloat.ApintMath
import kotlin.concurrent.thread
import kotlin.math.E
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


fun eulerBugChecker() = thread {
  val KCOMP_FOLDER = REGISTERED_FOLDER["kcomp"]
  val badEuler = KCOMP_FOLDER["KJ"]["v1"]["badEuler.txt"].text /*so that this kotlin file can be searched too*/
  KCOMP_FOLDER["KJ"].recursiveChildren()
	.filterHasExtension("kt")
	.forEach {
	  require(badEuler !in it.text) {
		"$badEuler is NOT what you think it is"
	  }
	}
}

@Serializable
data class Norm(
  val dX: Double,
  val dY: Double,
): ComputeInput<Double>() {
  //  fun loadCache() = loadCacheInline<Double,Norm>()
  override fun compute() = sqrt(dX.sq() + dY.sq())
}


private val module = SerializersModule {
  polymorphic(Any::class, Norm::class, Norm.serializer())
}.also {
  val oldMod = ComputeCache.jsonFormat.serializersModule
  println("oldMod = $oldMod")
  val newMod = oldMod + it
  println("newMod = $newMod")
  val oldFormat = ComputeCache.jsonFormat
  println("oldFormat=${oldFormat}")
  val newPolyFormat = ComputeCache.buildJsonFormat(newMod)
  ComputeCache.jsonFormat = newPolyFormat
  println("newPolyFormat=${newPolyFormat}")
  println("ComputeCache.jsonFormat=${ComputeCache.jsonFormat}")
}

@Serializable
data class Weight(
  val norm: Double,
  val sigmaPool: Double,
): ComputeInput<Double>() {
  //  override fun <T: ComputeInput<Double>> loadCache(): ComputeCache<T, Double> {
  //	TODO("Not yet implemented")
  //  }

  override fun compute() = E.pow((-(norm.sq()))/(2*sigmaPool.sq()))
}


@Serializable
data class Envelope(
  val gaussianEnveloped: Boolean, val xyt: XYThetas, val sigmaX: SigmaDenominator, val sigmaY: SigmaDenominator
): ComputeInput<Double>() {
  override fun compute() = (if (gaussianEnveloped) E.pow(
	-(xyt().xTheta.sq()/sigmaX()) - (xyt().yTheta.sq()/sigmaY())
  ) else 1.0)
}

@Serializable
data class SamplePhase(
  val xT: XYThetas, val SF: Double, val phase: PhaseType
): ComputeInput<Double>() {
  override fun compute() = phase.comp(
	xT().xTheta*SF
	/**2*PI*/
  ) /*cycles per degree*/
}

@Serializable
data class SigmaDenominator(
  val sigma: Double
): ComputeInput<Double>() {
  override fun compute() = (2.0*sigma.sq())
}

@Serializable
data class BayesianPriorC(
  val c0: Apfloat, val w0: Apfloat, val t: Apfloat
): ComputeInput<Apfloat>() {

  override fun compute() = run {
	val v = c0 - w0*cos(4.0.toApfloat()*t*API/180)
	require(v >= 0.0.toApfloat())
	v
  }

}

@Serializable
data class PPCUnit(
  val ft: Apfloat, val ri: /*Double*/Apint
): ComputeInput<Apfloat>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override fun compute() = run {
	/*val debugE = Apfloat(BIG_E)*//*.alsoPrintln { "debugE:${this}" }*/
	val debugNft = -ft /*Apfloat(BigDecimal(-ft))*//*.alsoPrintln { "debugNft:${this}" }*/
	val debugTopLeft = ApfloatMath.pow(Ae, debugNft)/*.alsoPrintln { "debugTopLeft:${this}" }*/    /*println("debugTopRight-ft=${ft}")
	  println("debugTopRight-ri=${ri}")*/    /*//	  println("ft1=$ft")
	  //	  println("ri1=$ri")*/
	val beforeBD = ft.pow(ri)
	val debugTopRight = beforeBD /*Apfloat(BigDecimal(beforeBD))*/
	val debugTop = debugTopLeft.multiply(debugTopRight)
	val debugBottom = ApintMath.factorial(ri.toLong())

	debugTop/debugBottom    /*(debugTop.divide(debugBottom)).toDouble() *//*NOT FLAT*/    /*debugTop.toDouble() *//*FLAT*/    /*debugBottom.toDouble() *//*FLAT*/
  }

}

@Serializable
data class LogPoissonPPCUnit(
  val ft: Double, val ri: /*Double*/Int
): ComputeInput<Double>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/


  override fun compute() = ln(e.pow(-ft)*ft.pow(ri)) - ri.logFactorial()
}

@Serializable
data class GPPCUnit(
  val ft: Double, val ri: /*Double*/Double
): ComputeInput<Double>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  companion object {
	fun ftToSigma(ft: Double) = sqrt(ft) /*from gaussian distribution*/
  }


  override fun compute() = run {
	val SIGMA = ftToSigma(ft) /*the only way to get a monotonic increase*/    /*1.0*/
	val NORMALIZER = 1/(sqrt(2*PI)*SIGMA)
	val DENOM = 2*SIGMA.sq()
	NORMALIZER*e.pow((-(ri - ft).sq()/DENOM))
  }
}

@Serializable
data class XYTheta(
  val xTheta: Double, val yTheta: Double
)

@Serializable
data class XYThetas(
  val tRadians: Double, val dX: Double, val dY: Double
): ComputeInput<XYTheta>() {


  override fun compute() = XYTheta(
	xTheta = cos(tRadians)*dX + sin(tRadians)*dY,

	/*here's an idea from experience... try not to delete the negative before this sin.*/
	yTheta = -sin(tRadians)*dX + cos(tRadians)*dY
  )
}

@Serializable
data class GaussianCoef(
  val a: Double, val b: Double, val c: Double
)

@Serializable
data class GaussianCoefCalculator(
  val points: List<BasicPoint>
): ComputeInput<GaussianCoef>() {

  companion object {
	val fitter by lazy { GaussianCurveFitter.create() }
  }

  override fun compute() = fitter.fit(points.map { WeightedObservedPoint(1.0, it.x, it.y) }).let {
	GaussianCoef(
	  a = it[0], b = it[1], c = it[2]
	)
  }
}

@Serializable
data class GaussianFit(
  val g: GaussianCoef, val xMin: Double, val xStep: Double, val xMax: Double
): ComputeInput<List<BasicPoint>>() {

  constructor(
	points: List<BasicPoint>, xMin: Double, xStep: Double, xMax: Double
  ): this(GaussianCoefCalculator(points).findOrCompute(), xMin = xMin, xStep = xStep, xMax = xMax)

  override fun compute() = run {
	val range = (xMin..xMax step xStep).toList()
	val array = mutableListOf<BasicPoint>()
	try {
	  range.forEach { x ->
		array += BasicPoint(
		  x = x, y = GaussianPoint(a = g.a, b = g.b, c = g.c, x = x).findOrCompute()
		)
	  }
	} catch (e: ConvergenceException) {
	  warn(e.message ?: "no message for $e")
	  e.printStackTrace()
	  array.clear()
	}
	array
  }

}

@Serializable
data class GaussianPoint(
  val a: Double, val b: Double, val c: Double, val x: Double
): ComputeInput<Double>() {

  override fun compute() = a*e.pow(-(x - b).sq()/(2.0*c.sq()))

}

@Serializable
data class Radians(
  val degrees: Double,
): ComputeInput<Double>() {

  override fun compute() = Math.toRadians(degrees)

}

@Serializable
data class ARadians(
  val degrees: Apfloat,
): ComputeInput<Apfloat>() {


  override fun compute() = ApfloatMath.toRadians(degrees)

}

@Serializable
data class Sin(
  val radians: Radians,
): ComputeInput<Double>() {

  override fun compute() = sin(radians())

}

@Serializable
data class Cos(
  val radians: Radians,
): ComputeInput<Double>() {


  override fun compute() = cos(radians())
}

@Serializable
data class ASin(
  val radians: ARadians,
): ComputeInput<Apfloat>() {


  override fun compute() = ApfloatMath.sin(radians())
}

@Serializable
data class ACos(
  val radians: ARadians,
): ComputeInput<Apfloat>() {


  override fun compute() = ApfloatMath.cos(radians())
}

@Serializable
data class Polar(
  val radius: Double, val rads: Radians
): ComputeInput<BasicPoint>() {


  override fun compute() = BasicPoint(
	x = radius*Cos(rads)(), y = radius*Sin(rads)()
  )
}

@Serializable
data class APolar(
  val radius: Apfloat, val rads: ARadians
): ComputeInput<APoint>() {

  override fun compute() = APoint(
	x = radius*ACos(rads)(), y = radius*ASin(rads)()
  )
}


infix fun Point.normTo(other: Point) = Norm(
  dX = xDouble - other.xDouble, dY = yDouble - other.yDouble
)

infix fun Point.normDistTo(other: Point) = normTo(other).findOrCompute()


/*enum class Phase {
  SIN, COS;

  fun comp(d: Double) = when (this) {
	SIN -> sin(d)
	COS -> cos(d)
  }
}*/

@Serializable
sealed class PhaseType {
  abstract fun comp(d: Double): Double

  object SIN: PhaseType() {
	override fun comp(d: Double) = sin(d)
  }

  object COS: PhaseType() {
	override fun comp(d: Double) = cos(d)
  }
}


@Serializable
data class Circle(val radius: Double, val center: BasicPoint = BasicPoint(0, 0)) {
  operator fun contains(p: BasicPoint): Boolean {

	return center.normDist(p) <= radius
  }

}

@Serializable
data class ACircle(val radius: Apfloat, val center: APoint = APoint(x = 0.0.toApfloat(), y = 0.0.toApfloat())) {
  operator fun contains(p: APoint): Boolean {

	return center.normDist(p) <= radius
  }

  operator fun contains(p: BasicPoint): Boolean {

	return center.normDist(p) <= radius
  }

}