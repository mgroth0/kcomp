package matt.sempart

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentData(
  val responses: Map<String, String>,
  /*val segment: String,
  val label: String,*/
  val trialLog: List<List<Pair<Long, String>>>,
)