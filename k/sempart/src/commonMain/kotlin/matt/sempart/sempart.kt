package matt.sempart

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentData(
  val responses: Map<String, String>,
  /*val segment: String,
  val label: String,*/
  /*"image" to im,*/
  val trialLog: List<Pair<Long, String>>,
)


