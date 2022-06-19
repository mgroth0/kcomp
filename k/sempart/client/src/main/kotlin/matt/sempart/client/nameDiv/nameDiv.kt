package matt.sempart.client.nameDiv

import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.column
import matt.kjs.css.JustifyContent.center
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.sempart.client.state.ExperimentPhase.Name
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.ui.ExperimentScreen

val nameDiv = ExperimentScreen(Name) {
  sty {
	display = flex
	flexDirection = column
	justifyContent = center
  }
  +"Please input your name:"
  var b: HTMLButtonWrapper? = null
  val t = textInput {
	sty.margin = MED_SPACE
	setOnInput {
	  b!!.enabled = value.isNotBlank()
	}
  }
  PhaseChange.beforeDispatch {
	if (it.second == Name) {
	  t.focus()
	}
  }
  b = button {
	sty.margin = MED_SPACE
	+"Confirm Name"
	disabled = true
	setOnClick {
	  Participant.pid = encodeURIComponent(t.value)
	  ExperimentState.nameIsGood.value = true
	}
  }
}

external fun encodeURIComponent(encodedURI: String): String
