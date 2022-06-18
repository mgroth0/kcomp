package matt.sempart.client.completeDiv

import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.kjs.prop.VarProp
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.sendData
import matt.sempart.client.ui.ExperimentScreen

val completeDiv = ExperimentScreen(Complete) {


  +"The experiment is complete. Thank you for your participation!"

  br
  br

  var b: HTMLButtonWrapper? = null

  +"Optionally, if you would like to give the researchers feedback on this experiment please submit it here."
  val ta = textArea {
	setOnInput {
	  b!!.enabled = true
	}
  }

  val sentFeedback = VarProp(false)

  span {
	b = button {
	  +"Submit Feedback"
	  enabled = false
	  setOnClick {
		enabled = false
		sendData(Feedback(ta.value))
	  }
	}
	sentFeedback.onChangeUntil({ it }, {
	  +"Feedback received. Thank you!"
	})

  }


  span {
	+"To confirm your completion of the study with Prolific (which necessary for payment) please "
	a {
	  href = COMPLETION_URL
	  +"click here"
	}
	+"."
  }

}