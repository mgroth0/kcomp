package matt.sempart.client.instructionsDiv.instructionsVid

import matt.kjs.css.TextAlign.center
import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.kjs.elements.br
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.source
import matt.kjs.elements.video
import matt.kjs.setOnClick
import matt.sempart.client.const.INSTRUCTIONS_VID_WOLFRAM
import matt.sempart.client.mainDivClass
import matt.sempart.client.state.ExperimentPhase.InstructionsVid
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.centerOnWindow

val instructionsVidDiv by lazy {
  div {
	classList.add(mainDivClass)
	onlyShowIn(InstructionsVid)
	sty.textAlign = center

	sty.centerOnWindow()
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