package matt.sempart.client.completeDiv

import matt.klib.css.Display.flex
import matt.klib.css.FlexDirection.column
import matt.klib.css.JustifyContent.center
import matt.klib.css.JustifyContent.spaceEvenly
import matt.klib.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.klib.prop.VarProp
import matt.klib.css.TextAlign
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.Participant.pid
import matt.sempart.client.state.sendData
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.sty.box
import matt.sempart.client.ui.ExperimentScreen
import matt.sempart.client.unixMSsessionID

val completeDiv = ExperimentScreen(Complete) {

  sty {
	display = flex
	flexDirection = column
	justifyContent = center
	textAlign = TextAlign.center
  }

  +"The experiment is complete. Thank you for your participation!"

  var b: HTMLButtonWrapper? = null

  div {
	sty {
	  box()
	  display = flex
	  flexDirection = column
	  justifyContent = spaceEvenly
	  textAlign = TextAlign.center
	  margin = MED_SPACE
	  padding = MED_SPACE
	  height = 400.px
	}
	+"Optionally, if you would like to give the researchers feedback on this experiment please submit it here."
	val ta = textArea {
	  sty {
		height = 300.px
	  }
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
		  sendData(Feedback(pid = pid, unixTimeMSsessionID = unixMSsessionID, feedback = ta.value)) {
			sentFeedback.value = true
		  }
		}
	  }
	  span {
		sty.opacity = 0
		sentFeedback.onChangeUntil({ it }, {
		  sty.opacity = 1
		})
		+"Feedback received. Thank you!"
	  }

	}
  }




  span {
	sty.margin = MED_SPACE
	+"To confirm your completion of the study with Prolific (which is necessary for payment) please "
	a {
	  href = COMPLETION_URL
	  +"click here"
	}
	+"."
  }

}