package matt.sempart.client.params

import kotlinx.serialization.Serializable
import matt.kjs.HOUR_MS
import matt.klib.todo



@Serializable
data class Params(
  val breakInterval: Int = 2,
  val removeNpButtonsKeepUnlabelledNpButtons: Boolean = true,
  val randomSegmentOrder: Boolean = true,
  val idleThreshold: Int = HOUR_MS
) {
  init {
	todo("HOUR_MS just for dev, use minute for final?")
  }
}

val PARAMS = Params()