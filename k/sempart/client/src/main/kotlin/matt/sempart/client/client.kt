package matt.sempart.client

import kotlinx.browser.document
import matt.kjs.Interval
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.every
import matt.kjs.html.elements.appendWrapper
import matt.kjs.html.elements.appendWrappers
import matt.kjs.html.elements.link
import matt.kjs.nextOrNull
import matt.sempart.SegmentResponse
import matt.sempart.TrialData
import matt.sempart.client.completeDiv.debugButton
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
import kotlin.time.Duration.Companion.milliseconds



fun main() = defaultMain {
  document.head!!.apply {
	title = "Semantic Segmentation"
	link {
	  rel = "stylesheet"
	  href = "style.css"
	}
  }
  document.body!!.sty {
	background = black
	color = white
  }

  
//  println("innerHTML5=${debugButton?.innerHTML}")
  println("innerHTML5.element=${debugButton?.element?.innerHTML}")
  document.body!!.appendWrappers(scaleInput, *SCREENS.toTypedArray())
//  println("innerHTML6=${debugButton?.innerHTML}")
  println("innerHTML6.element=${debugButton?.element?.innerHTML}")

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  /*need scale before creating elements*/
  PhaseChange.afterEndOfNext(Scaling) {
//	println("innerHTML7=${debugButton?.innerHTML}")
	println("innerHTML7.element=${debugButton?.element?.innerHTML}")
	println("innerHTML7.element.id=${debugButton?.element?.id}")
	fun presentImage(drawingData: DrawingData, training: Boolean = false) {
//	  println("innerHTML8=${debugButton?.innerHTML}")
	  println("innerHTML8.element=${debugButton?.element?.innerHTML}")
	  println("innerHTML8.element.id=${debugButton?.element?.id}")
	  val loadingProcess = DrawingLoadingProcess("downloading image data")
	  drawingData.whenReady {
//		println("innerHTML9=${debugButton?.innerHTML}")
		println("innerHTML9.element=${debugButton?.element?.innerHTML}")
		println("innerHTML9.element.id=${debugButton?.element?.id}")
		val trial = drawingData.trial.value!!
		trial.div.helpText.hidden = !training
		document.body!!.appendWrapper(trial.div)
		val nextDrawingData = imIterator.nextOrNull()?.let { DrawingData(it) }
		var interval: Interval? = null
		var width = 0
//		println("innerHTML10=${debugButton?.innerHTML}")
		println("innerHTML10.element=${debugButton?.element?.innerHTML}")
		println("innerHTML10.element.id=${debugButton?.element?.id}")
		trial.div.nextImageButton.apply {
		  setOnPointerDown {
			if (enabled) {
			  interval = every(10.milliseconds) {
				width += 1
				percent = width
				if (width == 100) {
				  interval!!.stop()
				  trial.registerInteraction("submit confirmed")
				  trial.cleanup()
				  if (training) presentImage(nextDrawingData!!)
				  else sendData(
					TrialData(
					  image = drawingData.baseImageName,
					  index = drawingData.idx,
					  responses = trial.segments.map { SegmentResponse(it.id, it.response!!) },
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
			}
		  }
		  setOnPointerUp {
			interval?.stop()
			width = 0
			percent = width
		  }
		}
		loadingProcess.finish()
		trial.log += "trial start"
	  }
	}
	presentImage(DrawingData(imIterator.next()), training = true)
  }
}





