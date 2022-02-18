package matt.v1.scaling

import matt.klib.log.warn
import matt.klib.math.sq
import matt.v1.lab.petri.PopulationConfig
import matt.v1.scaling.PerformanceMode.LONG_ACCURATE
import matt.v1.scaling.PerformanceMode.ORIG_BUT_NEEDS_GPU
import matt.v1.scaling.PerformanceMode.QUICK_ROUGH
import kotlin.math.abs
import kotlin.math.pow


enum class PerformanceMode {
  ORIG_BUT_NEEDS_GPU,
  LONG_ACCURATE,
  QUICK_ROUGH
}

val performanceMode = ORIG_BUT_NEEDS_GPU

fun PopulationConfig.rescaleSampleDensity(d: Double) = copy(
  sampleStep = sampleStep/d,
  baseContrast = baseContrast/(d.sq()),
  /*DC_BASELINE_ACTIVITY = DC_BASELINE_ACTIVITY*(d.sq())*10.0.pow(-5),
  semiSaturationConstant = semiSaturationConstant*(d.sq())*10.0.pow(-5)*/
).apply {
  require(performanceMode >= LONG_ACCURATE)
}

fun PopulationConfig.rescalePrefThetaDensity(d: Double) = copy(
  cellPrefThetaStep = cellPrefThetaStep/d,
  baseDNGain = baseDNGain/d,/* NOT ENOUGH BC MOST SUPPRESSION FROM NEARBY THETAS*/
  /*baseDNGain = baseDNGain*(1_000_000_000.0)*/
).apply {
  require(performanceMode >= QUICK_ROUGH)
  warn(
	"rescalePrefThetaDensity will unavoidably lead to much lower suppression because nearby thetas provided majority of suppression. Expect this to modify all results."
  )
}

fun PopulationConfig.rescaleCellSpatialRange(d: Double) = copy(
  cellX0AbsMinmax = cellX0AbsMinmax*d,
)

val DOUBLE_SAFE_PRECISION = Double.MIN_VALUE*10.0.pow(100)
val DOUBLE_SAFE_MAX = Double.MAX_VALUE/10.0.pow(100)
fun Double.requireIsSafe() {
  val a = abs(this)
  require(a < DOUBLE_SAFE_MAX && a > DOUBLE_SAFE_PRECISION)
}