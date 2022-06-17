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



object QueryParams {
  val PROLIFIC_PID = "PROLIFIC_PID"
  val SESSION_ID = "SESSION_ID"
  val STUDY_ID = "STUDY_ID"
  val preview = "preview"
}