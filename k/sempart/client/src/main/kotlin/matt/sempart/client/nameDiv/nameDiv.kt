package matt.sempart.client.nameDiv

import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.sempart.client.state.ExperimentPhase.Name
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.ui.ExperimentScreen

val nameDiv = ExperimentScreen(Name) {
  +"Please input your name:"
  var b: HTMLButtonWrapper? = null
  val t = textInput {
	setOnInput {
	  b!!.disabled = value.isNotBlank()
	}
  }
  b = button {
	disabled = true
	Participant.pid = encodeURIComponent(t.value)
	ExperimentState.nameIsGood.value = true
  }
}

external fun encodeURIComponent(encodedURI: String): String
