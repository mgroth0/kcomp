package matt.sempart.client.breakDiv

import kotlinx.html.ButtonType
import matt.css.Display.flex
import matt.css.FlexDirection.column
import matt.css.JustifyContent.center
import matt.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.css.TextAlign
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.ui.ExperimentScreen


val breakDiv = ExperimentScreen(
  Break
) {
  sty {
	display = flex
	flexDirection = column
	justifyContent = center
	textAlign = TextAlign.center
  }
  p {
	+"You may take a break and continue when you are ready."
	sty {
	  width = WIDTH.px
	}
  }
  button {
	sty.margin = MED_SPACE
	type = ButtonType.button.realValue
	+"Continue"
	setOnClick {
	  ExperimentState.interacted()
	  ExperimentState.onBreak.value = false
	}
  }
}