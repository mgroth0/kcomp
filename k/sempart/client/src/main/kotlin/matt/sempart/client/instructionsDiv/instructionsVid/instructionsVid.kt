package matt.sempart.client.instructionsDiv.instructionsVid

import matt.kjs.css.Position.absolute
import matt.kjs.css.TextAlign.center
import matt.kjs.css.auto
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.elements.br
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.source
import matt.kjs.elements.video
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_VID_WOLFRAM
import matt.sempart.client.state.ExperimentPhase.InstructionsVid
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn

val instructionsVidDiv by lazy {
  div {
	onlyShowIn(InstructionsVid)
	sty.textAlign = center

	/*https://stackoverflow.com/a/31029494/6596010*/
	sty {
	  position = absolute
	  top = 50.percent
	  left = 50.percent
	  transform = "translate(-50%,-50%)"
	}

	val vid = video {
	  source {
		src = INSTRUCTIONS_VID_WOLFRAM
		type = "video/mp4"
	  }
	}

	br

	button {
	  sty.margin = auto
	  innerHTML = "play/pause"
	  setOnClick {
		if (vid.paused) vid.play()
		else vid.pause()
	  }
	}

	button {
	  sty.margin = auto








	  innerHTML = "Click here when ready to move on"
	  setOnClick { ExperimentState.finishedVid = true }
	}



  }
}