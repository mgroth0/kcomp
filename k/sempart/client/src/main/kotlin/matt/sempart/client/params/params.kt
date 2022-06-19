package matt.sempart.client.params

import kotlinx.serialization.Serializable
import matt.klib.HOUR_MS
import matt.klib.MINUTE_MS
import matt.klib.todo



@Serializable
data class Params(
  val breakInterval: Int = 2,
  val removeNpButtonsKeepUnlabelledNpButtons: Boolean = true,
  val randomSegmentOrder: Boolean = true,
  val idleThresholdMS: Int = HOUR_MS,
  val idleCheckPeriodMS: Int = MINUTE_MS,
  val allowMultiSelection: Boolean = true
) {
  init {
	todo("HOUR_MS just for dev, use minute for final?")
  }
}

val PARAMS = Params()