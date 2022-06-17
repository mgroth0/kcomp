package matt.sempart.client.breakDiv

import kotlinx.html.ButtonType
import matt.kjs.css.auto
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.setOnClick
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.ui.ExperimentScreen


val breakDiv = ExperimentScreen(
  Break
) {
  p {
	sty {
	  width = WIDTH.px
	  margin = auto
	  innerHTML = "You may take a break and continue when you are ready."
	  button {
		type = ButtonType.button.realValue
		sty.margin = auto
		innerHTML = "Continue"
		setOnClick {
		  ExperimentState.interacted()
		  ExperimentState.onBreak = false
		}
	  }
	}
  }
}