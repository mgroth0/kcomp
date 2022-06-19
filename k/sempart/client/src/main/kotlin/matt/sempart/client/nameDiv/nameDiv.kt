package matt.sempart.client.nameDiv

import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.column
import matt.kjs.css.JustifyContent.spaceBetween
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.sempart.client.state.ExperimentPhase.Name
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.ui.ExperimentScreen

val nameDiv = ExperimentScreen(Name) {
  sty {
	display = flex
	flexDirection = column
	justifyContent = spaceBetween
  }
  +"Please input your name:"
  var b: HTMLButtonWrapper? = null
  val t = textInput {
	setOnInput {
	  b!!.disabled = value.isNotBlank()
	}
  }
  b = button {
	+"Confirm Name"
	disabled = true
	setOnClick {
	  Participant.pid = encodeURIComponent(t.value)
	  ExperimentState.nameIsGood.value = true
	}
  }
}

external fun encodeURIComponent(encodedURI: String): String
