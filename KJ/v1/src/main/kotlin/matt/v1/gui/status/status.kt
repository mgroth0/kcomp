package matt.v1.gui.status

import javafx.scene.layout.HBox
import javafx.scene.text.Font
import matt.async.date.sec
import matt.caching.every
import matt.gui.loop.runLaterReturn
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.tornadofx.control.label
import matt.kjlib.str.prependZeros
import matt.kjlib.stream.itr.loopIterator
import matt.v1.gui.status.StatusLabel.Status.IDLE
import matt.v1.gui.status.StatusLabel.Status.WORKING

class StatusLabel(
  val name: String? = null
): HBox()/*so label is steady*/ {


  enum class Status { WORKING, IDLE }

  var status = Prop(IDLE)
  val counters = mutableMapOf<String, Pair<Int, Int?>>()
  var statusExtra = ""

  private fun countersString(): String {
	return counters.entries.joinToString(separator = " ") {
	  val i = it.value.first
	  val total = it.value.second
	  val totalS = if (total != null) "/${total}" else ""
	  "${it.key} ${i.prependZeros(3)}${totalS}"
	}
  }

  init {
	prefWidth = 150.0 /*so label is steady*/
	layoutX = 0.0
	layoutY = 0.0
	val statusLabel = label("") {
	  font = Font.font("Monospaced")
	}


	val dotItr = (1..3).toList().loopIterator()
	every(0.05.sec, ownTimer = true) {
	  runLaterReturn {
		statusLabel.text =
		  status.value.toString() + " " + (if (name != null) "$name: " else "") + countersString() + " " + statusExtra + " " + when (status.value!!) {
			IDLE    -> ""
			WORKING -> (0 until dotItr.next()).joinToString(separator = "") { "." }
		  }
	  }
	}
  }
}



