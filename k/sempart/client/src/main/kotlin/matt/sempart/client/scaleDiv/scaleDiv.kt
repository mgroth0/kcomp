package matt.sempart.client.scaleDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.auto
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_IM_RELATIVE
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.ui.ExperimentScreen

val scaleDiv = ExperimentScreen(Scaling) {
  //  div {
  //	classList.add(mainDivClass)
  //	onlyShowIn(Scaling)
  sty {
	//	  display = flex
	//	  justifyContent = center
	//	  alignItems = AlignItems.center
	//	  flexDirection = column
	//	  textAlign = center
	//	  verticalAlign = VerticalAligns.middle
	height = 100.percent
  }

  img {
	src = INSTRUCTIONS_IM_RELATIVE
	//	  sty.verticalAlign = VerticalAligns.middle
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
  //  }
}