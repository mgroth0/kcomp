package matt.v1.model.activity

import matt.kjlib.jmath.toApfloat
import matt.v1.activity.DivNorm
import matt.v1.model.activity.WeightConfig.SigmaPooling
import kotlin.math.pow
import kotlin.math.sqrt

data class ActivityConfig(
  val baselineActivityDC: Double,
  val cfgDN: DivisiveNormConfig?
) {
  val baseDivNorm by lazy {
	cfgDN?.let {
	  DivNorm(
		D = null,
		c = cfgDN.gainC,
		v = cfgDN.semiSaturationConstant,
		S = null
	  )
	}
  }
}

data class DivisiveNormConfig(
  val gainC: Double,
  val semiSaturationConstant: Double,
  val weightCfg: WeightConfig,
)

sealed class WeightConfig {
  data class Uniform(val w: Double): WeightConfig()
  data class SigmaPooling(val s: Double): WeightConfig()
}




val ARI_BASELINE_B = 2.0
val DYNAMIC_BASELINE_B = 0.0.toApfloat()


val ARI_WEIGHT_CONFIG = SigmaPooling(sqrt(5.0))

val ARI_TD_DN_CFG = DivisiveNormConfig(
  gainC = 1.0*(10.0.pow(-4)),
  semiSaturationConstant = 1.0,
  weightCfg = ARI_WEIGHT_CONFIG
)
val ARI_ASD_DN_CFG = ARI_TD_DN_CFG.copy(
  gainC = ARI_TD_DN_CFG.gainC*0.75
)

val ARI_BASE_ACTIVITY_CFG = ActivityConfig(
  baselineActivityDC = ARI_BASELINE_B,
  cfgDN = ARI_TD_DN_CFG
)

/*val BASE_SIGMA_POOLING = *//*5.0*/
val ASD_SIGMA_POOLING = ARI_WEIGHT_CONFIG.s*0.8
val ATTENTION_SUPP_SIGMA_POOLING = ARI_WEIGHT_CONFIG.s*1.5