package matt.sempart.client.scaleDiv

import matt.kjs.css.TextAlign.center
import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.kjs.elements.br
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.img
import matt.kjs.elements.p
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
//import matt.sempart.client.mainDivClass
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.centerOnWindow

val scaleDiv by lazy {
  div {
//	classList.add(mainDivClass)
	onlyShowIn(Scaling)
	sty.textAlign = center

	sty.centerOnWindow()
	img {
	  src = INSTRUCTIONS_IM_RELATIVE
	}
	br
	p {
	  innerHTML =
		"Every computer is different, and some people use various accessibility settings to change scaling and zoom settings. In this experiment, we need everyone to see the images at the same size. Please use a ruler and adjust the slider above so the image is exactly 3 inches wide on your screen. The scaling you select here will be used throughout the experiment."
	}
	br
	button {
	  sty.margin = auto
	  innerHTML = "Click here when finished rescaling"
	  setOnClick { ExperimentState.finishedScaling = true }
	}
  }
}