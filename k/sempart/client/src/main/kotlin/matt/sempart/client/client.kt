package matt.sempart.client

import kotlinx.browser.document
import matt.kjs.Path
import matt.kjs.appendChilds
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.elements.appendWrapper
import matt.kjs.elements.input
import matt.kjs.ifConfirm
import matt.kjs.nextOrNull
import matt.kjs.req.post
import matt.sempart.ExperimentData
import matt.sempart.client.breakDiv.breakDiv
import matt.sempart.client.completeDiv.completeDiv
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.SEND_DATA_PREFIX
import matt.sempart.client.const.TRAIN_IM
import matt.sempart.client.const.TRIAL_CONFIRM_MESSAGE
import matt.sempart.client.inactiveDiv.inactiveDiv
import matt.sempart.client.instructionsDiv.instructionsDiv
import matt.sempart.client.instructionsDiv.instructionsVid.instructionsVidDiv
import matt.sempart.client.loadingDiv.DrawingLoadingProcess
import matt.sempart.client.loadingDiv.loadingDiv
import matt.sempart.client.params.PARAMS
import matt.sempart.client.resizeDiv.resizeDiv
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.trialdiv.div

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  document.body!!.sty {
	background = black
	color = white
  }
  document.body!!.appendChilds(
	instructionsVidDiv,
	instructionsDiv, resizeDiv, loadingDiv, completeDiv, breakDiv, inactiveDiv,
	input {
	  type = "range"
	  min = "0.5"
	  max = "2.0"
	  value = "1.0"
	  sty {
		width = 80.percent
	  }
	  oninput = {
		document.body!!.sty["transform"] = "scale(${value})"
		Unit
	  }
	}
  )

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  fun presentImage(drawingData: DrawingData, training: Boolean = false) {
	val loadingProcess = DrawingLoadingProcess("downloading image data")
	drawingData.whenReady {
	  val trial = drawingData.trial!!
	  trial.div.helpText.hidden = !training
	  document.body!!.appendWrapper(trial.div)
	  val nextDrawingData = imIterator.nextOrNull()?.let { DrawingData(it) }
	  trial.div.nextImageButton.onclick = trial.interaction("nextImageButton clicked") {
		ifConfirm(TRIAL_CONFIRM_MESSAGE) {
		  trial.registerInteraction("submit confirmed")
		  trial.cleanup()
		  if (training) presentImage(nextDrawingData!!)
		  else post(
			Path(SEND_DATA_PREFIX + Participant.pid),
			ExperimentData(
			  responses = trial.segments.associate { it.id to it.response!! },
			  trialLog = trial.log.get()
			)
		  ) {
			if (nextDrawingData != null) {
			  if ((nextDrawingData.idx - 1)%PARAMS.breakInterval == 0) {
				PhaseChange.afterEndOfNext(Break) {
				  presentImage(nextDrawingData)
				}
				ExperimentState.onBreak = true
			  } else presentImage(nextDrawingData)
			} else ExperimentState.complete = true
		  }
		}
	  }
	  loadingProcess.finish()
	  trial.log += "trial start"
	}
  }
  presentImage(DrawingData(imIterator.next()), training = true)
}





