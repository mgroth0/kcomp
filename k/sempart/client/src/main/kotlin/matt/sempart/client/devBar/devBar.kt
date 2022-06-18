package matt.sempart.client.devBar

import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.row
import matt.kjs.css.Position.absolute
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.html.elements.div
import matt.sempart.client.state.ExperimentPhase

val devBar by lazy {
  div {
	sty {
	  position = absolute
	  bottom = 50.px
	  display = flex
	  flexDirection = row
	  opacity = 0.25
	  zIndex = 1
	}

	ExperimentPhase.values().forEach { phase ->
	  button {
		+phase.name
		setOnClick {
		  ExperimentPhase.lockAt(phase)
		}
	  }
	}
  }
}