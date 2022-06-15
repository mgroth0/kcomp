package matt.sempart.client.instructionsDiv.instructionsVid

import matt.kjs.css.Margin.auto
import matt.kjs.css.TextAlign.center
import matt.kjs.css.sty
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.source
import matt.kjs.elements.video
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_VID_RELATIVE
import matt.sempart.client.state.ExperimentPhase.InstructionsVid
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn

val instructionsVidDiv by lazy {
  div {
	onlyShowIn(InstructionsVid)
	sty.textAlign = center


	val vid = video {
	  source {
		src = INSTRUCTIONS_VID_RELATIVE
		type = "video/mp4"
	  }
	}

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