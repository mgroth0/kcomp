package matt.sempart.client.instructionsDiv

import matt.kjs.css.auto
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Instructions
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.trialdiv.ImageAndControlsScreen

val instructionsDiv = ImageAndControlsScreen(Instructions) {
  stackDiv.img {
	src = INSTRUCTIONS_IM_RELATIVE
  }
  controlsDiv.p {
	sty {
	  width = WIDTH.px
	  margin = auto
	}
	+"""
			Please select the label that you think best matches each segment for
        each
        image. You may select segments either by clicking or by iterating through them with the "next" and
        "previous" buttons. All segments must be given a label before advancing to the next image.
		  """.trimIndent()
  }
  controlsDiv.button {
	sty.margin = auto
	+"Begin Experiment"
	setOnClick { ExperimentState.begun.value = true }
  }
}