package matt.sempart.client.instructionsDiv

import matt.kjs.css.TextAlign.center
import matt.kjs.css.auto
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.img
import matt.kjs.elements.p
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
import matt.sempart.client.const.WIDTH
//import matt.sempart.client.mainDivClass
import matt.sempart.client.state.ExperimentPhase.Instructions
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn

val instructionsDiv by lazy {
  div {
//	classList.add(mainDivClass)
	onlyShowIn(Instructions)
	sty.textAlign = center
	img {
	  src = INSTRUCTIONS_IM_RELATIVE
	}
	p {
	  sty {
		width = WIDTH.px
		margin = auto
	  }
	  innerHTML = """
			Please select the label that you think best matches each segment for
        each
        image. You may select segments either by clicking or by iterating through them with the "next" and
        "previous" buttons. All segments must be given a label before advancing to the next image.
		  """.trimIndent()
	}
	button {
	  sty.margin = auto
	  innerHTML = "Begin Experiment"
	  setOnClick { ExperimentState.begun = true }
	}
  }
}