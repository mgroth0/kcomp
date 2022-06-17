package matt.sempart

import kotlinx.serialization.Serializable

@Serializable
sealed class ExperimentData

@Serializable
class Issue(
  val unixTimeMS: Long,
  val message: String
): ExperimentData()

@Serializable
class Feedback(val feedback: String): ExperimentData()

@Serializable
class TrialData(
  val image: String,
  val index: Int,
  val responses: List<SegmentResponse>,
  val trialLog: List<LogMessage>,
): ExperimentData() {
  init {
	require(responses.map { it.segmentID }.toSet().size == responses.size)
  }
}

@Serializable
class SegmentResponse(
  val segmentID: String,
  val label: String
)

@Serializable
class LogMessage(
  val unixTimeMS: Long,
  val message: String
)


object QueryParams {
  val PROLIFIC_PID = "PROLIFIC_PID"
  val SESSION_ID = "SESSION_ID"
  val STUDY_ID = "STUDY_ID"
  val preview = "preview"
}