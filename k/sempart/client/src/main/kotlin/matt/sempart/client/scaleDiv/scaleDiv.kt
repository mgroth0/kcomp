package matt.sempart.client.scaleDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.Position.absolute
import matt.kjs.css.auto
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.handlers.setOnInput
import matt.kjs.html.elements.input
import matt.kjs.prop.VarProp
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.trialdiv.ImageAndControlsScreen

const val MIN_SCALE = 0.5
const val DEFAULT_SCALE = 1.0
const val MAX_SCALE = 1.5
val scaleProp = VarProp(DEFAULT_SCALE)
fun neededHeight() = scaleProp.value*HEIGHT
fun neededWidth() = scaleProp.value*ImageAndControlsScreen.TOTAL_WIDTH

val scaleDiv = ImageAndControlsScreen(Scaling) {
  stackDiv.img {
	src = INSTRUCTIONS_IM_RELATIVE
	height = HEIGHT
	width = WIDTH
  }
  controlsDiv.p {
	+"Every computer is different, and some people use various accessibility settings to change scaling and zoom settings. In this experiment, we need everyone to see the images at the same size. Please use a ruler and adjust the slider above so the image is exactly 3 inches wide on your screen. The scaling you select here will be used throughout the experiment."
	sty.marginBottom = MED_SPACE
  }
  controlsDiv.button {
	sty.margin = auto
	+"Click here when finished rescaling"
	setOnClick { ExperimentState.finishedScaling = true }
  }
}


val scaleInput by lazy {
  input {
	PhaseChange.afterEndOfNext(Scaling) {
	  hidden = true
	}
	type = "range"
	step = "0.01"
	min = MIN_SCALE.toString()
	defaultValue = DEFAULT_SCALE.toString()
	value = DEFAULT_SCALE.toString()
	max = MAX_SCALE.toString()
	sty {
	  width = 80.percent
	  position = absolute
	  left = 10.percent
	  top = 10.percent
	  zIndex = 1
	}
	setOnInput {
	  scaleProp.value = value.toDouble()
	}
  }
}