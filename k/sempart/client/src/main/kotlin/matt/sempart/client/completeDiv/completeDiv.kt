package matt.sempart.client.completeDiv

import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.sendData
import matt.sempart.client.ui.ExperimentScreen

var debugButton: HTMLButtonWrapper? = null

val completeDiv = ExperimentScreen(Complete) {


  +"The experiment is complete. Thank you for your participation!"

  br
  br

  var b: HTMLButtonWrapper? = null

  +"Optionally, if you would like to give the researchers feedback on this experiment please submit it here."
  val ta = textArea {
	println("configuring textArea")
	setOnInput {
	  println("in value changed")
	  b!!.enabled = true
	  println("b!!.enabled=${b!!.enabled}")
	}
	println("configured textArea")
  }
  b = button {
	+"Submit Feedback"
	enabled = false
	setOnClick {
	  enabled = false
	  sendData(Feedback(ta.value))
	}
  }
  debugButton = b
  b.id = "debugButton"
  +"To confirm your completion of the study with Prolific (which necessary for payment) please "
  a {
	href = COMPLETION_URL
	+"click here"
  }


}