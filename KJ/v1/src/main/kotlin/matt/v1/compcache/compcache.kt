@file:Suppress("ProtectedInFinal")

package matt.v1.compcache

import matt.kjlib.async.every
import matt.kjlib.async.parAssociateWith
import matt.kjlib.async.with
import matt.kjlib.date.sec
import matt.kjlib.jmath.API
import matt.kjlib.jmath.Ae
import matt.kjlib.jmath.ApE
import matt.kjlib.jmath.PI
import matt.kjlib.jmath.cos
import matt.kjlib.jmath.div
import matt.kjlib.jmath.e
import matt.kjlib.jmath.logFactorial
import matt.kjlib.jmath.minus
import matt.kjlib.jmath.plus
import matt.kjlib.jmath.pow
import matt.kjlib.jmath.sq
import matt.kjlib.jmath.sqrt
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.jmath.unaryMinus
import matt.kjlib.ranges.step
import matt.kjlib.str.addSpacesUntilLengthIs
import matt.kjlib.str.tab
import matt.kjlib.str.truncate
import matt.klib.log.warn
import matt.klib.math.sq
import matt.klibexport.klibexport.setAll
import matt.reflect.subclasses
import matt.v1.lab.globalStatusLabel
import matt.v1.lab.petri.Population
import matt.v1.model.ComplexCell
import matt.v1.model.Phase
import matt.v1.model.PopulationResponse
import matt.v1.model.Response
import matt.v1.model.Stimulus
import org.apache.commons.math3.exception.ConvergenceException
import org.apache.commons.math3.fitting.GaussianCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import org.apfloat.Apint
import org.apfloat.ApintMath
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import java.util.concurrent.Semaphore
import kotlin.collections.MutableMap.MutableEntry
import kotlin.math.E
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.full.companionObjectInstance

const val MAX_CACHE_SIZE = 1_000_000
const val PRINT_REPORTS = false

/*val e = *//*= eFloat*/

class MutSemMap<K, V>(
  private val map: MutableMap<K, V> = HashMap(),
  private val maxsize: Int = Int.MAX_VALUE
): MutableMap<K, V> {
  private val sem = Semaphore(1)
  override val size: Int
	get() = sem.with { map.size }

  override fun containsKey(key: K): Boolean {
	return sem.with { map.containsKey(key) }
  }

  override fun containsValue(value: V): Boolean {
	return sem.with { map.containsValue(value) }
  }

  override fun get(key: K): V? {
	return sem.with { map.get(key) }
  }

  override fun isEmpty(): Boolean {
	return sem.with { map.isEmpty() }
  }

  override val entries: MutableSet<MutableEntry<K, V>>
	get() = sem.with { map.entries }
  override val keys: MutableSet<K>
	get() = sem.with { map.keys }
  override val values: MutableCollection<V>
	get() = sem.with { map.values }

  override fun clear() {
	sem.with { map.clear() }
  }

  override fun put(key: K, value: V): V? {
	return sem.with { map.put(key, value) }
  }

  override fun putAll(from: Map<out K, V>) {
	return sem.with { map.putAll(from) }
  }

  override fun remove(key: K): V? {
	return sem.with { map.remove(key) }
  }

  fun setIfNotFull(k: K, v: V): Boolean {
	return sem.with {
	  if (map.size < maxsize) {
		map[k] = v
		true
	  } else false
	}
  }

}

fun <K, V> mutSemMapOf(vararg pairs: Pair<K, V>, maxsize: Int = Int.MAX_VALUE) =
  MutSemMap(mutableMapOf(*pairs), maxsize = maxsize)


abstract class ComputeCache<I, O>(val enableCache: Boolean = true) {
  var full = false


  val computeCache = mutSemMapOf<I, O>(maxsize = MAX_CACHE_SIZE)
  abstract val compute: I.()->O
}

abstract class ComputeInput<I, O> {
  companion object {

	init {
	  if (PRINT_REPORTS) {
		every(5.sec) {
		  println("ComputeCache Report")
		  tab("Name\t\tSize\t\tFull")
		  ComputeInput::class.subclasses().forEach {
			val cache = (it.companionObjectInstance as ComputeCache<*, *>)
			val s = if (cache.enableCache) cache.computeCache.size else "DISABLED"
			tab(
			  "${it.simpleName!!.addSpacesUntilLengthIs(30).truncate(30)}\t\t${s}\t\t${cache.full}"
			)
		  }
		}
	  }
	}
  }

  operator fun invoke() = findOrCompute()
  abstract val computer: ComputeCache<I, O>
  fun findOrCompute(): O {
	return if (!computer.enableCache) {
	  @Suppress("UNCHECKED_CAST") computer.compute.invoke(this as I)
	} else run {
	  @Suppress("UNCHECKED_CAST") var r = computer.computeCache[this as I]
	  if (r == null) {
		@Suppress("UNCHECKED_CAST") if (this is SigmaDenominator) {
		  println("SIGMA DENOM NOT FOUND: ${this.sigma}")
		  println("IN:")
		  computer.computeCache.forEach {
			tab(it.key.sigma)
		  }
		}
		r = computer.compute.invoke(this as I)
		if (!computer.full) {
		  computer.full = !computer.computeCache.setIfNotFull(this, r)
		}
	  }
	  r!!
	}
  }
}

data class Norm(
  val dX: Double,
  val dY: Double,
): ComputeInput<Norm, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Norm, Double>() {
	override val compute: Norm.()->Double = { sqrt(dX.sq() + dY.sq()) }
  }
}

data class Weight(
  val norm: Apfloat,
  val sigmaPool: Apfloat,
): ComputeInput<Weight, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Weight, Double>() {
	override val compute: Weight.()->Double = {
	  ApE.pow((-(norm.sq()))/(2*sigmaPool.sq())).toDouble()
	}
  }
}

data class DivNorm(
  val D: Apfloat?, val c: Apfloat, /*Suppressive Field Gain*/
  val v: Apfloat, /*SemiSaturation Constant*/
  val S: Apfloat? /*Suppressive Field*/
): ComputeInput<DivNorm, Apfloat>() {
  override val computer = Companion

  companion object: ComputeCache<DivNorm, Apfloat>() {
	override val compute: DivNorm.()->Apfloat = {
	  D!!/(v + c*S!!)
	}
  }
}


data class Envelope(
  val gaussianEnveloped: Boolean, val xyt: XYThetas, val sigmaX: SigmaDenominator, val sigmaY: SigmaDenominator
): ComputeInput<Envelope, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Envelope, Double>() {
	override val compute: Envelope.()->Double = {
	  (if (gaussianEnveloped) E.pow(
		-(xyt().xTheta.sq()/sigmaX()) - (xyt().yTheta.sq()/sigmaY())
	  ) else 1.0)
	}
  }
}

data class SamplePhase(
  val xT: XYThetas, val SF: Double, val phase: Phase
): ComputeInput<SamplePhase, Double>() {
  override val computer = Companion

  companion object: ComputeCache<SamplePhase, Double>() {
	override val compute: SamplePhase.()->Double = {
	  phase.comp(
		xT().xTheta*SF
		/**2*PI*/
	  ) /*cycles per degree*/
	}
  }
}

data class SigmaDenominator(
  val sigma: Double
): ComputeInput<SigmaDenominator, Double>() {
  override val computer = Companion

  companion object: ComputeCache<SigmaDenominator, Double>() {
	override val compute: SigmaDenominator.()->Double = {
	  (2.0*sigma.sq())
	}
  }
}

data class AttentionAmp(
  val norm: Norm
): ComputeInput<AttentionAmp, Double>() {
  override val computer = Companion

  companion object: ComputeCache<AttentionAmp, Double>() {
	private val G = 7.0
	private val SIGMA_ATTENTION = 2.0
	override val compute: AttentionAmp.()->Double = {
	  1 + G*e.pow(-((norm.findOrCompute().sq())/(2*SIGMA_ATTENTION.sq())))
	}
  }
}

data class BayesianPriorC(
  val c0: Apfloat, val w0: Apfloat, val t: Apfloat
): ComputeInput<BayesianPriorC, Apfloat>() {
  override val computer = Companion

  companion object: ComputeCache<BayesianPriorC, Apfloat>() {
	override val compute: BayesianPriorC.()->Apfloat = {
	  val v = c0 - w0*cos(4.0.toApfloat()*t*API/180)
	  require(v >= 0.0.toApfloat())
	  v
	}
  }
}

data class PPCUnit(
  val ft: Apfloat, val ri: /*Double*/Apint
): ComputeInput<PPCUnit, Apfloat>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  companion object: ComputeCache<PPCUnit, Apfloat>() {
	override val compute: PPCUnit.()->Apfloat =
	  {    /*val debugE = Apfloat(BIG_E)*//*.alsoPrintln { "debugE:${this}" }*/
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
}

data class LogPoissonPPCUnit(
  val ft: Double, val ri: /*Double*/Int
): ComputeInput<LogPoissonPPCUnit, Double>() {

  /*constructor(ft: Apfloat, ri: Apint): this(ft = ft, ri = ri.roundToInt())*/

  override val computer = Companion


  companion object: ComputeCache<LogPoissonPPCUnit, Double>() {
	override val compute: LogPoissonPPCUnit.()->Double = {
	  ln(e.pow(-ft)*ft.pow(ri)) - ri.logFactorial()
	}
  }
}

data class GPPCUnit(
  val ft: Double, val ri: /*Double*/Double
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
  val attention: Boolean = false,
  val pop: Population,
  val uniformW: Apfloat? = null,
  val rawInput: Apfloat? = null,
  val ti: Apint? = null,
  val h: Apfloat? = null,
  val lastPopR: PopulationResponse? = null,
  val sigmaPooling: Apfloat,
  val divNorm: DivNorm
): ComputeInput<MaybePreDNPopR, PopulationResponse>() {
  override val computer = Companion


  companion object: ComputeCache<MaybePreDNPopR, PopulationResponse>() {
	override val compute: MaybePreDNPopR.()->PopulationResponse = {

	  /*val t = tic(prefix = "getting PopulationResponse")*/    /*println("computing MaybePreDNPopR for a=${stim.a}")*/

	  val total = pop.complexCells.size

	  var i = 0
	  val sem = Semaphore(1)
	  pop.complexCells.asSequence()        /*.onEveryIndexed(10) { i: Int, _: Cell ->
		  coreLoopForStatusUpdates.outer.statusLabel.update(i = i, total = total)
		}*/        /*.associateWith() {*/.parAssociateWith {        /*t.toc("starting ${it.hashCode()}")*/
		val r = Stimulation(
		  cell = it,
		  stim = stim,
		  uniformW = uniformW,
		  rawInput = rawInput,
		  attention = attention,
		  popR = lastPopR,
		  ti = ti,
		  h = h,
		)()        /*t.toc("got r for ${it.hashCode()}")*/
		sem.with {
		  i++
		  if (i%10 == 0) {            /*coreLoopForStatusUpdates.outer.statusLabel*/
			globalStatusLabel!!.counters["stimulated cell"] = i to total
		  }
		}        /*t.toc("past sem for ${it.hashCode()}")*/
		r
	  }.let {        /*t.toc("ok finally")*/        /*println("done computing PopulationResponse")*/
		PopulationResponse(
		  m = it, sigmaPooling = sigmaPooling, divNorm = divNorm
		)
	  }
	}
  }
}

data class Stimulation(
  val cell: ComplexCell,
  val stim: Stimulus,
  val popR: PopulationResponse? = null,
  val attention: Boolean = false,
  val uniformW: Apfloat? = null,
  val rawInput: Apfloat? = null,
  val ti: Apint? = null,
  val h: Apfloat? = null,
): ComputeInput<Stimulation, Response>() {
  override val computer = Companion


  companion object: ComputeCache<Stimulation, Response>() {
	override val compute: Stimulation.()->Response = {
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


data class XYTheta(
  val xTheta: Double, val yTheta: Double
)

data class XYThetas(
  val tRadians: Double, val dX: Double, val dY: Double
): ComputeInput<XYThetas, XYTheta>() {
  override val computer = Companion

  companion object: ComputeCache<XYThetas, XYTheta>() {
	override val compute: XYThetas.()->XYTheta = {
	  XYTheta(
		xTheta = cos(tRadians)*dX + sin(tRadians)*dY,

		/*here's an idea from experience... try not to delete the negative before this sin.*/
		yTheta = -sin(tRadians)*dX + cos(tRadians)*dY
	  )
	}
  }
}

data class GaussianCoef(
  val a: Double, val b: Double, val c: Double
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
		  a = it[0], b = it[1], c = it[2]
		)
	  }
	}
  }
}

data class Point(val x: Double, val y: Double) {
  fun normDist(other: Point) = sqrt((x - other.x).sq() + (y - other.y).sq())
  fun toAPoint() = APoint(x = x.toApfloat(), y = y.toApfloat())
}

data class APoint(val x: Apfloat, val y: Apfloat) {
  fun normDist(other: APoint) = sqrt((x - other.x).sq() + (y - other.y).sq())
  fun normDist(other: Point) = normDist(other.toAPoint())
}

val Collection<Point>.trough get() = minByOrNull { it.y }
val Collection<Point>.gradient get() = (maxOf { it.y } - minOf { it.y })/(maxOf { it.x } - minOf { it.x })

fun List<Point>.normalizeYToMax(): List<Point> {
  val max = maxOf { it.y }
  return map { it.copy(y = it.y/max*100.0.toDouble()) }
}

fun List<Point>.derivative(n: Int = 1): List<Point> {/*could make this recursive but functionally equivalent*/
  require(n > -1)
  if (n == 0) return this
  var d = this
  repeat((1..n).count()) {
	d = if (d.size < 2) emptyList()
	else d.subList(1, d.size).mapIndexed { index, point ->
	  Point(x = point.x, y = point.y - d[index].y)
	}
  }
  return d
}

fun List<Point>.normalizeYToMinMax(): List<Point> {
  val min = minOf { it.y }
  val max = maxOf { it.y } - min
  return map { it.copy(y = (it.y - min)/max*100.0) }
}

fun List<Point>.normalizeXToMax(): List<Point> {
  val max = maxOf { it.x }
  return map { it.copy(x = it.x/max*100.0.toDouble()) }
}

fun List<Point>.normalizeXToMinMax(): List<Point> {
  val min = minOf { it.x }
  val max = maxOf { it.x } - min
  return map { it.copy(x = (it.x - min)/max*100.0) }
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
  val g: GaussianCoef, val xMin: Double, val xStep: Double, val xMax: Double
): ComputeInput<GaussianFit, List<Point>>() {

  constructor(
	points: List<Point>, xMin: Double, xStep: Double, xMax: Double
  ): this(GaussianCoefCalculator(points).findOrCompute(), xMin = xMin, xStep = xStep, xMax = xMax)

  override val computer = Companion


  companion object: ComputeCache<GaussianFit, List<Point>>() {
	override val compute: GaussianFit.()->List<Point> = {
	  val range = (xMin..xMax step xStep).toList()
	  val array = mutableListOf<Point>()
	  try {
		range.forEach { x ->
		  array += Point(
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
}

data class GaussianPoint(
  val a: Double, val b: Double, val c: Double, val x: Double
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
	  sin(radians())
	}
  }
}

data class Cos(
  val radians: Radians,
): ComputeInput<Cos, Double>() {
  override val computer = Companion

  companion object: ComputeCache<Cos, Double>() {
	override val compute: Cos.()->Double = {
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
  val radius: Double, val rads: Radians
): ComputeInput<Polar, Point>() {
  override val computer = Companion

  companion object: ComputeCache<Polar, Point>() {
	override val compute: Polar.()->Point = {
	  Point(
		x = radius*Cos(rads)(), y = radius*Sin(rads)()
	  )
	}
  }
}

data class APolar(
  val radius: Apfloat, val rads: ARadians
): ComputeInput<APolar, APoint>() {
  override val computer = Companion

  companion object: ComputeCache<APolar, APoint>() {
	override val compute: APolar.()->APoint = {
	  APoint(
		x = radius*ACos(rads)(), y = radius*ASin(rads)()
	  )
	}
  }
}
