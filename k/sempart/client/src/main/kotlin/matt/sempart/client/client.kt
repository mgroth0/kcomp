package matt.sempart.client

import kotlinx.browser.document
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.html.elements.appendWrapper
import matt.kjs.html.elements.appendWrappers
import matt.kjs.html.elements.body.wrapped
import matt.kjs.html.elements.head.wrapped
import matt.kjs.nextOrNull
import matt.kjs.prop.whenTrueOnce
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.TRAIN_IM
import matt.sempart.client.loadingDiv.DrawingLoadingProcess
import matt.sempart.client.params.PARAMS
import matt.sempart.client.scaleDiv.scaleInput
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.sendData
import matt.sempart.client.trialdiv.div
import matt.sempart.client.ui.SCREENS
import org.w3c.dom.HTMLBodyElement


fun main() = defaultMain {
  document.head!!.wrapped().apply {
	title = "Semantic Segmentation"
	link {
	  rel = "stylesheet"
	  href = "style.css"
	}
  }.configure()
  (document.body!! as HTMLBodyElement).wrapped().sty {
	background = black
	color = white
  }


  document.body!!.appendWrappers(scaleInput, *SCREENS.toTypedArray())

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  PhaseChange.afterEndOfNext(Scaling) {
	fun presentImage(drawingData: DrawingData) {
//	  gitPush again and again
	  val loadingProcess = DrawingLoadingProcess("downloading image data")
	  drawingData.ready.whenTrueOnce {
		val trial = drawingData.trial.value!!
		document.body!!.appendWrapper(trial.div)
		val nextDrawingData = imIterator.nextOrNull()?.let { DrawingData(it, training = false) }
		trial.div.nextImageButton.setOnFullHold {
		  trial.registerInteraction("submit confirmed")
		  trial.div.element.remove()
		  if (trial.training) presentImage(nextDrawingData!!)
		  else sendData(trial.data()) {
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
		loadingProcess.finish()
		trial.log += "trial start"
	  }
	}
	presentImage(DrawingData(imIterator.next(), training = true))
  }
}





