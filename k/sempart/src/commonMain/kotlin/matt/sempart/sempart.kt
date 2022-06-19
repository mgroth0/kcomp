package matt.sempart

import kotlinx.serialization.Serializable

interface HasPid {
  val pid: String
}

@Serializable
sealed class ExperimentData(): HasPid

@Serializable
class Issue(
  override val pid: String,
  val unixTimeMS: Long,
  val message: String
): ExperimentData()

@Serializable
class Feedback(override val pid: String, val feedback: String): ExperimentData()

@Serializable
class TrialData(
  override val pid: String,
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

@Serializable
class FinalData(
  val participants: MutableList<Participant> = mutableListOf()
)

@Serializable
class Participant(
  val pid: String,
  val drawings: MutableList<Drawing> = mutableListOf(),
  val feedback: MutableList<String> = mutableListOf(),
  val issues: MutableList<SimpleIssue> = mutableListOf()
)

@Serializable
class Drawing(
  val image: String,
  val responses: MutableList<SegmentResponse> = mutableListOf(),
  val log: MutableList<LogMessage> = mutableListOf()
)

@Serializable
class SimpleIssue(
  val unixTimeMS: Long,
  val message: String
)