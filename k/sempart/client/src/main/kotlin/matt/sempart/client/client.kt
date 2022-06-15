package matt.sempart.client

import kotlinx.browser.document
import kotlinx.browser.window
import matt.kjs.Path
import matt.kjs.appendChilds
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.elements.appendWrapper
import matt.kjs.req.post
import matt.sempart.ExperimentData
import matt.sempart.client.breakDiv.breakDiv
import matt.sempart.client.completeDiv.completeDiv
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.inactiveDiv.inactiveDiv
import matt.sempart.client.instructionsDiv.instructionsDiv
import matt.sempart.client.loadingDiv.DrawingLoadingProcess
import matt.sempart.client.loadingDiv.loadingDiv
import matt.sempart.client.params.PARAMS
import matt.sempart.client.resizeDiv.resizeDiv
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.trialdiv.div
import kotlin.js.Date

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  document.body!!.sty {
	background = black
	color = white
  }
  document.body!!.appendChilds(
	instructionsDiv, resizeDiv, loadingDiv, completeDiv, breakDiv, inactiveDiv
  )

  val images = ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  fun presentImage(drawingData: DrawingData) {
	val loadingProcess = DrawingLoadingProcess("downloading image data")
	drawingData.whenReady {
	  val trial = drawingData.trial!!
	  val trialDiv = trial.div
	  document.body!!.appendWrapper(trialDiv)
	  trial.log.add(Date.now().toLong() to "trial start")
	  val nextDrawingData = imIterator.takeIf { it.hasNext() }?.let { DrawingData(it.next()) }
	  trialDiv.nextImageButton.onclick = trial.interaction("next image button clicked") {
		if (window.confirm("Are you sure you are ready to proceed? You cannot go back to this image.")) {
		  trial.registerInteraction("submit confirmed")
		  trialDiv.nextImageButton.disabled = true
		  trial.cleanup()
		  post(
			Path("send?PROLIFIC_PID=${Participant.pid}"),
			ExperimentData(
			  responses = trial.segments.associate { it.id to it.response!! },            /*"image" to im,*/
			  trialLog = trial.log
			)
		  ) {
			if (nextDrawingData != null) {
			  if (nextDrawingData.idx%PARAMS.breakInterval == 0) {
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
	}
  }
  presentImage(DrawingData(imIterator.next()))

  window.addEventListener("resize", {
	PhaseChange.dispatchToAllHTML(ExperimentPhase.determine().let { it to it })
  })
}





