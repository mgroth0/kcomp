package matt.v1.model.stim

data class StimulusConfig(
  val baseContrast: Double,
  val stimSF: Double,
  val stimSigma: Double,
)

val rosenbergBaseStimConfig = StimulusConfig(
  baseContrast = 1.0,
  stimSF = 5.75,/*/ (2 * PI),*/ /*... see emails*/
  stimSigma = 1.55
)