package matt.sempart.client.devBar

import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.row
import matt.kjs.css.Position.absolute
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.html.elements.div
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.PhaseChange

val devBar by lazy {
  div {
	sty {
	  position = absolute
	  bottom = 100.px
	  display = flex
	  flexDirection = row
	  opacity = 0.25
	}

	ExperimentPhase.values().forEach {
	  button {
		+it.name
		setOnClick {
		  PhaseChange
		}
	  }
	}
  }
}