package matt.sempart.client.instructionsDiv.instructionsVid

import matt.kjs.css.Display.flex
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.handlers.setOnClick
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.sempart.client.const.INSTRUCTIONS_VID_WOLFRAM
import matt.sempart.client.state.ExperimentPhase.InstructionsVid
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.trialdiv.ImageAndControlsScreen

val instructionsVidDiv = ImageAndControlsScreen(InstructionsVid) {
  stackDiv.sty {
	display = flex
  }

  controlsDiv.p {
	+"Please turn up your volume so you can hear the video."
  }

  var playPauseButton: HTMLButtonWrapper? = null
  var moveOnButton: HTMLButtonWrapper? = null
  val vid = stackDiv.video {
	sty.width = 100.percent
	source {
	  src = INSTRUCTIONS_VID_WOLFRAM
	  type = "video/mp4"
	}
	setOnEnded {
	  playPauseButton!!.disabled = false
	  moveOnButton!!.disabled = false
	}
	/*PhaseChange.atStartOf(InstructionsVid) {
	  play()
	}*/
  }
  playPauseButton = controlsDiv.button {
	sty.width = 100.percent
	//	disabled = true
	+"play/pause"
	setOnClick {
	  if (vid.paused) vid.play()
	  else vid.pause()
	}
  }
  moveOnButton = controlsDiv.button {
	sty.width = 100.percent
	disabled = true
	+"Click here when you are ready to move on"
	setOnClick {
	  vid.pause()
	  ExperimentState.finishedVid.value = true
	}
  }
}