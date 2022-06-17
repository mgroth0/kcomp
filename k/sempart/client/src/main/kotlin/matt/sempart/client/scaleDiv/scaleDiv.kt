package matt.sempart.client.scaleDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.Position.absolute
import matt.kjs.css.auto
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.elements.input
import matt.kjs.prop.VarProp
import matt.kjs.setOnClick
import matt.kjs.setOnInput
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.ui.ExperimentScreen

const val DEFAULT_SCALE = "1.0"

val scaleDiv = ExperimentScreen(Scaling) {
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

val scaleProp = VarProp(DEFAULT_SCALE)

val scaleInput by lazy {
  input {
	PhaseChange.afterEndOfNext(Scaling) {
	  hidden = true
	}
	type = "range"
	step = "0.01"
	min = "0.5"
	defaultValue = DEFAULT_SCALE
	value = DEFAULT_SCALE
	max = "1.5"
	sty {
	  width = 80.percent
	  position = absolute
	  left = 10.percent
	  top = 10.percent
	  zIndex = 1
	}
	setOnInput {
	  scaleProp.value = value
	}
  }
}