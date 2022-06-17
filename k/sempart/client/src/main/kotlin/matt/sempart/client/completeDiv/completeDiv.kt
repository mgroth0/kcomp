package matt.sempart.client.completeDiv

import matt.kjs.html.elements.enabled
import matt.kjs.props.valueProperty
import matt.kjs.setOnClick
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.sendData
import matt.sempart.client.ui.ExperimentScreen
import org.w3c.dom.HTMLButtonElement



val completeDiv = ExperimentScreen(Complete) {
  +"The experiment is complete. Thank you for your participation!"

  br
  br

  var b: HTMLButtonElement? = null

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

  +"To confirm your completion of the study with Prolific (which necessary for payment) please "
  a {
	href = COMPLETION_URL
	+"click here"
  }
}