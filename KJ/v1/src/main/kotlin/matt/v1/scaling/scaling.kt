package matt.v1.scaling

import matt.klib.log.warn
import matt.klib.math.sq
import matt.v1.lab.petri.PopulationConfig
import matt.v1.model.combined.CombinedConfig
import matt.v1.scaling.PerformanceMode.DEBUG
import matt.v1.scaling.PerformanceMode.LONG_ACCURATE
import matt.v1.scaling.PerformanceMode.ORIG_BUT_NEEDS_GPU
import matt.v1.scaling.PerformanceMode.QUICK_ROUGH
import kotlin.math.abs
import kotlin.math.pow

fun CombinedConfig.copyWith(perfMode: PerformanceMode) = when (perfMode) {
  ORIG_BUT_NEEDS_GPU -> this
  LONG_ACCURATE      -> this.rescaleSampleDensity(0.75)
	.run { copy(populationConfig = populationConfig.copy(reqSize = null)) }
  QUICK_ROUGH        -> this.rescalePrefThetaDensity(0.1).rescaleSampleDensity(0.5)
	.run { copy(populationConfig = populationConfig.copy(reqSize = null)) }
  DEBUG              -> this.rescalePrefThetaDensity(0.1).rescaleSampleDensity(0.1)
	.run { copy(populationConfig = populationConfig.copy(reqSize = null)) }
}

enum class PerformanceMode {
  ORIG_BUT_NEEDS_GPU,
  LONG_ACCURATE,
  QUICK_ROUGH,
  DEBUG
}

fun CombinedConfig.rescaleSampleDensity(d: Double) = copy(
  fieldConfig = fieldConfig.copy(sampleStep = fieldConfig.sampleStep/d),
  stimConfig = stimConfig.copy(baseContrast = stimConfig.baseContrast/(d.sq())),
  /*DC_BASELINE_ACTIVITY = DC_BASELINE_ACTIVITY*(d.sq())*10.0.pow(-5),
  semiSaturationConstant = semiSaturationConstant*(d.sq())*10.0.pow(-5)*/
)

fun CombinedConfig.rescalePrefThetaDensity(d: Double) = copy(

  populationConfig = populationConfig.copy(
	cellPrefThetaStep = populationConfig.cellPrefThetaStep/d,
  ),

  activityConfig = activityConfig.copy(
	cfgDN = activityConfig.cfgDN?.copy(
	  gainC = activityConfig.cfgDN.gainC/d
	) /* NOT ENOUGH BC MOST SUPPRESSION FROM NEARBY THETAS*/
  ),

  //  cellPrefThetaStep = cellPrefThetaStep/d,
  //  baseDNGain = baseDNGain/d,
  /*baseDNGain = baseDNGain*(1_000_000_000.0)*/
).apply {
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


