package matt.sempart.client

import kotlinx.browser.document
import matt.kjs.Interval
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.Position.absolute
import matt.kjs.css.Transform.Scale
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.elements.appendWrapper
import matt.kjs.elements.appendWrappers
import matt.kjs.elements.input
import matt.kjs.elements.link
import matt.kjs.every
import matt.kjs.nextOrNull
import matt.kjs.setOnInput
import matt.sempart.Issue
import matt.sempart.SegmentResponse
import matt.sempart.TrialData
import matt.sempart.client.breakDiv.breakDiv
import matt.sempart.client.completeDiv.completeDiv
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.TRAIN_IM
import matt.sempart.client.inactiveDiv.inactiveDiv
import matt.sempart.client.instructionsDiv.instructionsDiv
import matt.sempart.client.instructionsDiv.instructionsVid.instructionsVidDiv
import matt.sempart.client.loadingDiv.DrawingLoadingProcess
import matt.sempart.client.loadingDiv.LoadingDiv
import matt.sempart.client.params.PARAMS
import matt.sempart.client.resizeDiv.resizeDiv
import matt.sempart.client.scaleDiv.scaleDiv
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.sendData
import matt.sempart.client.trialdiv.div
import kotlin.js.Date
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


  val divs = listOf(
	scaleDiv,
	instructionsVidDiv,
	instructionsDiv,
	resizeDiv,
	LoadingDiv,
	completeDiv,
	breakDiv,
	inactiveDiv
  )

  require(divs.size == ExperimentPhase.values().size - 1) {
	"did you forget to add a new div to the div list?"
  }

  val defaultScale = "1.0"

  document.body!!.appendChild(
	input {
	  PhaseChange.afterEndOfNext(Scaling) {
		hidden = true
	  }
	  type = "range"
	  step = "0.01"
	  min = "0.5"
	  defaultValue = defaultScale
	  value = defaultScale
	  max = "1.5"
	  sty {
		width = 80.percent
		position = absolute
		left = 10.percent
		top = 10.percent
		zIndex = 1
	  }
	  setOnInput {
		divs.forEach {
		  it.sty.resetTransform {
			scale(value.toDouble())
		  }
		}
	  }
	}
  )
  document.body!!.appendWrappers(
	*divs.toTypedArray()
  )


  /*neccesary so first trialDiv doesnt get NPE, or if slider is never moved*/
  divs.forEach {
	it.sty.resetTransform {
	  scale(defaultScale.toDouble())
	}
  }

  PhaseChange.beforeDispatch {
	if (it.second == Inactive) {
	  sendData(Issue(Date.now().toLong(), "participant went idle and experiment was cancelled"))
	}
  }

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  /*need scale before creating elements*/
  PhaseChange.afterEndOfNext(Scaling) {
	fun presentImage(drawingData: DrawingData, training: Boolean = false) {
	  println("presenting image of $drawingData")
	  val loadingProcess = DrawingLoadingProcess("downloading image data")
	  drawingData.whenReady {
		println("in whenReady of $drawingData")
		val trial = drawingData.trial.value!!
		trial.div.element.sty.resetTransform {
		  scale(scaleDiv.sty.transform.funs.filterIsInstance<Scale>().first().args)
		}
		trial.div.helpText.hidden = !training
		document.body!!.appendWrapper(trial.div)
		val nextDrawingData = imIterator.nextOrNull()?.let { DrawingData(it) }
		var interval: Interval? = null
		var width = 0
		trial.div.nextImageButton.apply {
		  trial.div.nextImageButton.setOnPointerDown {
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
		/*trial.div.nextImageButton.onclick = trial.interaction("nextImageButton clicked") {

		  ifConfirm(TRIAL_CONFIRM_MESSAGE) {

		  }
		}*/
		loadingProcess.finish()
		trial.log += "trial start"
	  }
	}
	presentImage(DrawingData(imIterator.next()), training = true)
  }
}





