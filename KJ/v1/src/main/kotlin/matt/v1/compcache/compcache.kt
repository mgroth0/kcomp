@file:Suppress("ProtectedInFinal")

package matt.v1.compcache

import matt.kjlib.async.parAssociateWith
import matt.kjlib.jmath.Ae
import matt.kjlib.jmath.PIFloat
import matt.kjlib.jmath.div
import matt.kjlib.jmath.eFloat
import matt.kjlib.jmath.logFactorialFloat
import matt.kjlib.jmath.minus
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.pow
import matt.kjlib.jmath.sq
import matt.kjlib.jmath.sqrt
import matt.kjlib.jmath.times
import matt.kjlib.jmath.unaryMinus
import matt.kjlib.ranges.step
import matt.kjlib.str.tab
import matt.kjlib.stream.onEveryIndexed
import matt.klib.dmap.withStoringDefault
import matt.klib.math.sq
import matt.klibexport.klibexport.setAll
import matt.v1.lab.Experiment.CoreLoop
import matt.v1.lab.petri.Population
import matt.v1.model.ComplexCell
import matt.v1.model.PopulationResponse
import matt.v1.model.Stimulus
import org.apache.commons.math3.fitting.GaussianCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import org.apfloat.Apint
import org.apfloat.ApintMath
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val e = eFloat

abstract class ComputeCache<I, O> {
  val disableCache = false
  val computeCache = mutableMapOf<I, O>().withStoringDefault {
	compute(it)
  }
  abstract val compute: I.()->O
}

abstract class ComputeInput<I, O> {
  operator fun invoke() = findOrCompute()
  protected abstract val computer: ComputeCache<I, O>
  fun findOrCompute(debug: Boolean = false): O {
	return if (!debug) {
	  if (computer.disableCache) {
		computer.compute.invoke(this as I)
	  } else computer.computeCache[this as I]!!
	} else {
	  println("DEBUG:${this}")
	  tab("disableCache=${computer.disableCache}")
	  tab("in=${(this as I) in computer.computeCache}")
	  val r = if (computer.disableCache) {
		computer.compute.invoke(this as I)
	  } else computer.computeCache[this]!!
	  tab("r=${r}")
	  r
	}
  }
}

data class Norm(
  val dX: Float,
  val dY: Float,
): ComputeInput<Norm, Float>() {
  override val computer = Companion

  protected companion object: ComputeCache<Norm, Float>() {
	override val compute: Norm.()->Float = { sqrt(dX.sq() + dY.sq()) }
  }
}

data class Weight(
  val norm: Float,
  val sigmaPool: Float,
): ComputeInput<Weight, Float>() {
  override val computer = Companion

  protected companion object: ComputeCache<Weight, Float>() {
	override val compute: Weight.()->Float = {
	  eFloat.pow((-(norm.sq()))/(2*sigmaPool.sq()))
	}
  }
}

data class DivNorm(
  val D: Float?,
  val c: Float, /*Suppressive Field Gain*/
  val v: Float, /*SemiSaturation Constant*/
  val S: Float? /*Suppressive Field*/
): ComputeInput<DivNorm, Float>() {
  override val computer = Companion

  protected companion object: ComputeCache<DivNorm, Float>() {
	override val compute: DivNorm.()->Float = {
	  D!!/(v + c*S!!)
	}
  }
}

data class AttentionAmp(
  val norm: Norm
): ComputeInput<AttentionAmp, Float>() {
  override val computer = Companion

  protected companion object: ComputeCache<AttentionAmp, Float>() {
	private val G = 7.0.toFloat()
	private val SIGMA_ATTENTION = 2.0.toFloat()
	override val compute: AttentionAmp.()->Float = {
	  1 + G*eFloat.pow(-((norm.findOrCompute().sq())/(2*SIGMA_ATTENTION.sq())))
	}
  }
}

data class BayesianPriorC(
  val c0: Float,
  val w0: Float,
  val t: Float
): ComputeInput<BayesianPriorC, Float>() {
  override val computer = Companion

  protected companion object: ComputeCache<BayesianPriorC, Float>() {
	override val compute: BayesianPriorC.()->Float = {
	  val v = c0 - w0*cos(4.0.toFloat()*t*PIFloat/180)
	  require(v >= 0)
	  v
	}
  }
}

data class PPCUnit(
  val ft: Apfloat,
  val ri: /*Double*/Apint
): ComputeInput<PPCUnit, Apfloat>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  protected companion object: ComputeCache<PPCUnit, Apfloat>() {
	override val compute: PPCUnit.()->Apfloat = {
	  /*val debugE = Apfloat(BIG_E)*//*.alsoPrintln { "debugE:${this}" }*/
	  val debugNft = -ft /*Apfloat(BigDecimal(-ft))*//*.alsoPrintln { "debugNft:${this}" }*/
	  val debugTopLeft = ApfloatMath.pow(Ae, debugNft)/*.alsoPrintln { "debugTopLeft:${this}" }*/
	  /*println("debugTopRight-ft=${ft}")
	  println("debugTopRight-ri=${ri}")*/
	  /*//	  println("ft1=$ft")
	  //	  println("ri1=$ri")*/
	  val beforeBD = ft.pow(ri)
	  val debugTopRight = beforeBD /*Apfloat(BigDecimal(beforeBD))*/
	  val debugTop = debugTopLeft.multiply(debugTopRight)
	  val debugBottom = ApintMath.factorial(ri.toLong())

	  debugTop/debugBottom
	  /*(debugTop.divide(debugBottom)).toDouble() *//*NOT FLAT*/
	  /*debugTop.toDouble() *//*FLAT*/
	  /*debugBottom.toDouble() *//*FLAT*/
	}
  }
}

data class LogPoissonPPCUnit(
  val ft: Float,
  val ri: /*Double*/Int
): ComputeInput<LogPoissonPPCUnit, Float>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  protected companion object: ComputeCache<LogPoissonPPCUnit, Float>() {
	override val compute: LogPoissonPPCUnit.()->Float = {
	  ln(eFloat.pow(-ft)*ft.pow(ri)) - ri.logFactorialFloat()
	}
  }
}

data class GPPCUnit(
  val ft: Float,
  val ri: /*Double*/Float
): ComputeInput<GPPCUnit, Float>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  companion object: ComputeCache<GPPCUnit, Float>() {
	fun ftToSigma(ft: Float) = sqrt(ft) /*from gaussian distribution*/
	override val compute: GPPCUnit.()->Float = {
	  val SIGMA = ftToSigma(ft) /*the only way to get a monotonic increase*/    /*1.0*/
	  val NORMALIZER = 1/(sqrt(2*PIFloat)*SIGMA)
	  val DENOM = 2*SIGMA.sq()
	  NORMALIZER*e.pow((-(ri - ft).sq()/DENOM))
	}
  }
}

data class MaybePreDNPopR(
  val stim: Stimulus,
  val attention: Boolean,
  val pop: Population,
  val uniformW: Float?,
  val rawInput: Float?,
  val ti: Int? = null,
  val h: Float? = null,
  val lastPopR: PopulationResponse? = null
): ComputeInput<MaybePreDNPopR, PopulationResponse>() {
  override val computer = Companion


  companion object: ComputeCache<MaybePreDNPopR, PopulationResponse>() {
	lateinit var coreLoopForStatusUpdates: CoreLoop
	override val compute: MaybePreDNPopR.()->PopulationResponse = {


	  pop.complexCells
		.asSequence()
		.onEveryIndexed(10) { i, _ -> coreLoopForStatusUpdates.update(i = i) }
		/*.associateWith() {*/
		.parAssociateWith() {
		  it.stimulate(
			stim,
			uniformW = uniformW,
			rawInput = rawInput,
			attention = attention,
			popR = lastPopR,
			ti = ti,
			h = h,
		  )
		}.let {
		  PopulationResponse(
			m = it
		  )
		}
	}
  }
}

data class Stimulation(
  val cell: ComplexCell,
  val stim: Stimulus,
  val popR: PopulationResponse?,
  val attention: Boolean,
  val uniformW: Float?,
  val rawInput: Float?,
  val ti: Int? = null,
  val h: Float? = null,
): ComputeInput<Stimulation, Pair<Float, Float?>>() {
  override val computer = Companion


  companion object: ComputeCache<Stimulation, Pair<Float, Float?>>() {
	override val compute: Stimulation.()->Pair<Float, Float?> = {
	  cell.stimulate(
		stim = stim,
		uniformW = uniformW,
		rawInput = rawInput,
		attention = attention,
		popR = popR,
		ti = ti,
		h = h,
	  )
	}
  }
}

data class XTheta(
  val t: Float,
  val dX: Float,
  val dY: Float
): ComputeInput<XTheta, Float>() {
  override val computer = Companion


  companion object: ComputeCache<XTheta, Float>() {
	override val compute: XTheta.()->Float = {
	  cos(t)*dX + sin(t)*dY
	}
  }
}

data class YTheta(
  val t: Float,
  val dX: Float,
  val dY: Float
): ComputeInput<YTheta, Float>() {
  override val computer = Companion


  companion object: ComputeCache<YTheta, Float>() {
	override val compute: YTheta.()->Float = {
	  -sin(t)*dX + cos(t)*dY
	}
  }
}

data class GaussianCoef(
  val a: Float,
  val b: Float,
  val c: Float
)

data class GaussianCoefCalculator(
  val points: List<Point>
): ComputeInput<GaussianCoefCalculator, GaussianCoef>() {
  override val computer = Companion

  companion object: ComputeCache<GaussianCoefCalculator, GaussianCoef>() {
	val fitter by lazy { GaussianCurveFitter.create() }
	override val compute: GaussianCoefCalculator.()->GaussianCoef = {
	  fitter.fit(points.map { WeightedObservedPoint(1.0, it.x.toDouble(), it.y.toDouble()) }).let {
		GaussianCoef(
		  a = it[0].toFloat(),
		  b = it[1].toFloat(),
		  c = it[2].toFloat()
		)
	  }
	}
  }
}

data class Point(val x: Float, val y: Float) {
  fun normDist(other: Point) = sqrt((x - other.x).sq() + (y - other.y).sq())
}

data class APoint(val x: Apfloat, val y: Apfloat) {
  fun normDist(other: APoint) = sqrt((x - other.x).sq() + (y - other.y).sq())
}

val Collection<Point>.trough get() = minByOrNull { it.y }
val Collection<Point>.gradient get() = (maxOf { it.y } - minOf { it.y })/(maxOf { it.x } - minOf { it.x })

fun List<Point>.normalizedToMax(): List<Point> {
  val max = maxOf { it.y }
  return map { it.copy(y = it.y/max*100.0.toFloat()) }
}

fun List<Point>.normalizedToMinMax(): List<Point> {
  val min = minOf { it.y }
  val max = maxOf { it.y } - min
  return map { it.copy(y = (it.y - min)/max*100.0.toFloat()) }
}

fun Iterable<MutableList<Point>>.maxByTroughY() = maxByOrNull { it.trough!!.y }!!
fun Iterable<MutableList<Point>>.minByTroughY() = minByOrNull { it.trough!!.y }!!
fun Iterable<MutableList<Point>>.shiftAllByTroughs() {
  val higherTrough = maxByTroughY()
  val lowerTrough = minByTroughY()
  filter { it != lowerTrough }.forEach {
	it.setAll(higherTrough.map { it.copy(y = it.y - (higherTrough.trough!!.y - lowerTrough.trough!!.y)) })
  }
}


data class GaussianFit(
  val g: GaussianCoef,
  val xMin: Float,
  val xStep: Float,
  val xMax: Float
): ComputeInput<GaussianFit, List<Point>>() {

  constructor(
	points: List<Point>,
	xMin: Float,
	xStep: Float,
	xMax: Float
  ): this(GaussianCoefCalculator(points).findOrCompute(), xMin = xMin, xStep = xStep, xMax = xMax)

  override val computer = Companion


  companion object: ComputeCache<GaussianFit, List<Point>>() {
	override val compute: GaussianFit.()->List<Point> = {
	  val range = (xMin..xMax step xStep).toList()
	  val array = mutableListOf<Point>()
	  range.forEach { x ->
		array += Point(
		  x = x,
		  y = GaussianPoint(a = g.a, b = g.b, c = g.c, x = x).findOrCompute()
		)
	  }
	  array
	}
  }
}

data class GaussianPoint(
  val a: Float,
  val b: Float,
  val c: Float,
  val x: Float
): ComputeInput<GaussianPoint, Float>() {
  override val computer = Companion


  companion object: ComputeCache<GaussianPoint, Float>() {
	override val compute: GaussianPoint.()->Float = {
	  a*eFloat.pow(-(x - b).sq()/(2.0.toFloat()*c.sq()))
	}
  }
}


data class Radians(
  val degrees: Float,
): ComputeInput<Radians, Float>() {
  override val computer = Companion

  companion object: ComputeCache<Radians, Float>() {
	override val compute: Radians.()->Float = {
	  Math.toRadians(degrees.toDouble()).toFloat()
	}
  }
}

data class ARadians(
  val degrees: Apfloat,
): ComputeInput<ARadians, Apfloat>() {
  override val computer = Companion

  companion object: ComputeCache<ARadians, Apfloat>() {
	override val compute: ARadians.()->Apfloat = {
	  ApfloatMath.toRadians(degrees)
	}
  }
}

data class Sin(
  val radians: Radians,
): ComputeInput<Sin, Float>() {
  override val computer = Companion

  companion object: ComputeCache<Sin, Float>() {
	override val compute: Sin.()->Float = {
	  sin(radians())
	}
  }
}

data class Cos(
  val radians: Radians,
): ComputeInput<Cos, Float>() {
  override val computer = Companion

  companion object: ComputeCache<Cos, Float>() {
	override val compute: Cos.()->Float = {
	  cos(radians())
	}
  }
}


data class ASin(
  val radians: ARadians,
): ComputeInput<ASin, Apfloat>() {
  override val computer = Companion

  companion object: ComputeCache<ASin, Apfloat>() {
	override val compute: ASin.()->Apfloat = {
	  ApfloatMath.sin(radians())
	}
  }
}

data class ACos(
  val radians: ARadians,
): ComputeInput<ACos, Apfloat>() {
  override val computer = Companion

  companion object: ComputeCache<ACos, Apfloat>() {
	override val compute: ACos.()->Apfloat = {
	  ApfloatMath.cos(radians())
	}
  }
}

data class Polar(
  val radius: Float,
  val rads: Radians
): ComputeInput<Polar, Point>() {
  override val computer = Companion

  companion object: ComputeCache<Polar, Point>() {
	override val compute: Polar.()->Point = {
	  Point(x = radius*Cos(rads)(), y = radius*Sin(rads)())
	}
  }
}

data class APolar(
  val radius: Apfloat,
  val rads: ARadians
): ComputeInput<APolar, APoint>() {
  override val computer = Companion

  companion object: ComputeCache<APolar, APoint>() {
	override val compute: APolar.()->APoint = {
	  APoint(x = radius*ACos(rads)(), y = radius*ASin(rads)())
	}
  }
}
