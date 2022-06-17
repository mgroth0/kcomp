package matt.sempart.client.completeDiv

import matt.kjs.handlers.setOnClick
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.kjs.props.valueProperty
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.sendData
import matt.sempart.client.ui.ExperimentScreen

var debugButton: HTMLButtonWrapper? = null

val completeDiv = ExperimentScreen(Scaling) {



  +"The experiment is complete. Thank you for your participation!"

  br
  br

  var b: HTMLButtonWrapper? = null

  +"Optionally, if you would like to give the researchers feedback on this experiment please submit it here."
  val ta = textArea {
	valueProperty().onChange {
	  b!!.enabled = true
	}
  }
  b = button {
	+"Submit Feedback"
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