@file:Suppress("ProtectedInFinal")

package matt.v1.compcache

import matt.kjlib.async.parChunkAssociateWith
import matt.kjlib.jmath.Ae
import matt.kjlib.jmath.PI
import matt.kjlib.jmath.div
import matt.kjlib.jmath.e
import matt.kjlib.jmath.logFactorial
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
import kotlin.math.sqrt

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
  val dX: Double,
  val dY: Double,
): ComputeInput<Norm, Double>() {
  override val computer = Companion

  protected companion object: ComputeCache<Norm, Double>() {
	override val compute: Norm.()->Double = { sqrt(dX.sq() + dY.sq()) }
  }
}

data class Weight(
  val norm: Double,
  val sigmaPool: Double,
): ComputeInput<Weight, Double>() {
  override val computer = Companion

  protected companion object: ComputeCache<Weight, Double>() {
	override val compute: Weight.()->Double = {
	  e.pow((-(norm.sq()))/(2*sigmaPool.sq()))
	}
  }
}

data class DivNorm(
  val D: Double?,
  val c: Double, /*Suppressive Field Gain*/
  val v: Double, /*SemiSaturation Constant*/
  val S: Double? /*Suppressive Field*/
): ComputeInput<DivNorm, Double>() {
  override val computer = Companion

  protected companion object: ComputeCache<DivNorm, Double>() {
	override val compute: DivNorm.()->Double = {
	  D!!/(v + c*S!!)
	}
  }
}

data class AttentionAmp(
  val norm: Norm
): ComputeInput<AttentionAmp, Double>() {
  override val computer = Companion

  protected companion object: ComputeCache<AttentionAmp, Double>() {
	private val G = 7.0
	private val SIGMA_ATTENTION = 2.0
	override val compute: AttentionAmp.()->Double = {
	  1 + G*e.pow(-((norm.findOrCompute().sq())/(2*SIGMA_ATTENTION.sq())))
	}
  }
}

data class BayesianPriorC(
  val c0: Double,
  val w0: Double,
  val t: Double
): ComputeInput<BayesianPriorC, Double>() {
  override val computer = Companion

  protected companion object: ComputeCache<BayesianPriorC, Double>() {
	override val compute: BayesianPriorC.()->Double = {
	  val v = c0 - w0*cos(4.0*t*PI/180)
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
  val ft: Double,
  val ri: /*Double*/Int
): ComputeInput<LogPoissonPPCUnit, Double>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  protected companion object: ComputeCache<LogPoissonPPCUnit, Double>() {
	override val compute: LogPoissonPPCUnit.()->Double = {
	  ln(e.pow(-ft)*ft.pow(ri)) - ri.logFactorial()
	}
  }
}

data class GPPCUnit(
  val ft: Double,
  val ri: /*Double*/Double
): ComputeInput<GPPCUnit, Double>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  companion object: ComputeCache<GPPCUnit, Double>() {
	fun ftToSigma(ft: Double) = sqrt(ft) /*from gaussian distribution*/
	override val compute: GPPCUnit.()->Double = {
	  val SIGMA = ftToSigma(ft) /*the only way to get a monotonic increase*/    /*1.0*/
	  val NORMALIZER = 1/(sqrt(2*PI)*SIGMA)
	  val DENOM = 2*SIGMA.sq()
	  NORMALIZER*e.pow((-(ri - ft).sq()/DENOM))
	}
  }
}

data class MaybePreDNPopR(
  val stim: Stimulus,
  val attention: Boolean,
  val pop: Population,
  val uniformW: Double?,
  val rawInput: Double?,
  val ti: Int? = null,
  val h: Double? = null,
  val lastPopR: PopulationResponse? = null
): ComputeInput<MaybePreDNPopR, PopulationResponse>() {
  override val computer = Companion


  companion object: ComputeCache<MaybePreDNPopR, PopulationResponse>() {
	lateinit var coreLoopForStatusUpdates: CoreLoop
	override val compute: MaybePreDNPopR.()->PopulationResponse = {


	  pop.complexCells
		.asSequence()
		.onEveryIndexed(10) { i, _ -> coreLoopForStatusUpdates.update(i = i) }
		.parChunkAssociateWith(numThreads = 20) {
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
  val uniformW: Double?,
  val rawInput: Double?,
  val ti: Int? = null,
  val h: Double? = null,
): ComputeInput<Stimulation, Pair<Double, Double?>>() {
  override val computer = Companion


  companion object: ComputeCache<Stimulation, Pair<Double, Double?>>() {
	override val compute: Stimulation.()->Pair<Double, Double?> = {
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
  val t: Double,
  val dX: Double,
  val dY: Double
): ComputeInput<XTheta, Double>() {
  override val computer = Companion


  companion object: ComputeCache<XTheta, Double>() {
	override val compute: XTheta.()->Double = {
	  (Math.cos(t)*(dX) + Math.sin(t)*(dY))
	}
  }
}

data class YTheta(
  val t: Double,
  val dX: Double,
  val dY: Double
): ComputeInput<YTheta, Double>() {
  override val computer = Companion


  companion object: ComputeCache<YTheta, Double>() {
	override val compute: YTheta.()->Double = {
	  (-Math.sin(t)*(dX) + Math.cos(t)*(dY))
	}
  }
}

data class GaussianCoef(
  val a: Double,
  val b: Double,
  val c: Double
)

data class GaussianCoefCalculator(
  val points: List<Point>
): ComputeInput<GaussianCoefCalculator, GaussianCoef>() {
  override val computer = Companion

  companion object: ComputeCache<GaussianCoefCalculator, GaussianCoef>() {
	val fitter by lazy { GaussianCurveFitter.create() }
	override val compute: GaussianCoefCalculator.()->GaussianCoef = {
	  fitter.fit(points.map { WeightedObservedPoint(1.0, it.x, it.y) }).let {
		GaussianCoef(
		  a = it[0],
		  b = it[1],
		  c = it[2]
		)
	  }
	}
  }
}

data class Point(val x: Double, val y: Double) {
  fun normDist(other: Point) = sqrt((x - other.x).sq() + (y - other.y).sq())
}

data class APoint(val x: Apfloat, val y: Apfloat) {
  fun normDist(other: APoint) = sqrt((x - other.x).sq() + (y - other.y).sq())
}

val Collection<Point>.trough get() = minByOrNull { it.y }
val Collection<Point>.gradient get() = (maxOf { it.y } - minOf { it.y })/(maxOf { it.x } - minOf { it.x })

fun List<Point>.normalizedToMax(): List<Point> {
  val max = maxOf { it.y }
  return map { it.copy(y = it.y/max*100.0) }
}

fun List<Point>.normalizedToMinMax(): List<Point> {
  val min = minOf { it.y }
  val max = maxOf { it.y } - min
  return map { it.copy(y = (it.y - min)/max*100.0) }
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
  val xMin: Double,
  val xStep: Double,
  val xMax: Double
): ComputeInput<GaussianFit, List<Point>>() {

  constructor(
	points: List<Point>,
	xMin: Double,
	xStep: Double,
	xMax: Double
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
  val a: Double,
  val b: Double,
  val c: Double,
  val x: Double
): ComputeInput<GaussianPoint, Double>() {
  override val computer = Companion


  companion object: ComputeCache<GaussianPoint, Double>() {
	override val compute: GaussianPoint.()->Double = {
	  a*e.pow(-(x - b).sq()/(2.0*c.sq()))
	}
  }
}


data class Radians(
  val degrees: Double,
): ComputeInput<Radians, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Radians, Double>() {
	override val compute: Radians.()->Double = {
	  Math.toRadians(degrees)
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
): ComputeInput<Sin, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Sin, Double>() {
	override val compute: Sin.()->Double = {
	  Math.sin(radians())
	}
  }
}

data class Cos(
  val radians: Radians,
): ComputeInput<Cos, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Cos, Double>() {
	override val compute: Cos.()->Double = {
	  Math.cos(radians())
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
  val radius: Double,
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
