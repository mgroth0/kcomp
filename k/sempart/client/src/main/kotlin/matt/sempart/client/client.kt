package matt.sempart.client

import kotlinx.browser.document
import matt.kjs.bindings.not
import matt.klib.css.Color.black
import matt.klib.css.Color.white
import matt.kjs.css.sty
import matt.kjs.currentTimeMillis
import matt.kjs.defaultMain
import matt.kjs.html.elements.appendWrapper
import matt.kjs.html.elements.appendWrappers
import matt.kjs.html.elements.body.wrapped
import matt.kjs.html.elements.head.wrapped
import matt.klib.nextOrNull
import matt.klib.prop.whenTrueOnce
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.PILOT_PID
import matt.sempart.client.const.TRAIN_IM
import matt.sempart.client.devBar.devBar
import matt.sempart.client.loadingDiv.DrawingLoadingProcess
import matt.sempart.client.params.PARAMS
import matt.sempart.client.scaleDiv.scaleInput
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.ExperimentState.nameIsGood
import matt.sempart.client.state.Participant.pid
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.sendData
import matt.sempart.client.trialdiv.div
import matt.sempart.client.ui.SCREENS
import org.w3c.dom.HTMLBodyElement

private const val DEV_MODE = false
val unixMSsessionID = currentTimeMillis()

fun main() = defaultMain {

  ExperimentState.nameIsGood.value = pid != PILOT_PID
  println("nameIsGood1=${nameIsGood.value}")

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
  if (DEV_MODE) document.body!!.appendWrapper(devBar)

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  PhaseChange.afterEndOfNext(Scaling) {
	println("nameIsGood2=${nameIsGood.value}")
	fun presentImage(drawingData: DrawingData) {
	  val loadingProcess = DrawingLoadingProcess("downloading image data")
	  loadingProcess.start()
	  drawingData.ready.whenTrueOnce {
		val trial = drawingData.trial.value!!
		document.body!!.appendWrapper(trial.div)
		val nextDrawingData = imIterator.nextOrNull()?.let { DrawingData(it, training = false) }
		trial.div.nextImageButton.setOnFullHold {
		  trial.registerInteraction("submit confirmed")
		  trial.div.element.remove()
		  if (trial.training) presentImage(nextDrawingData!!)
		  else {
			sendData(trial.data())
			if (nextDrawingData != null) {
			  if ((nextDrawingData.idx - 1)%PARAMS.breakInterval == 0) {
				presentImage(nextDrawingData)
				ExperimentState.onBreak.value = true
			  } else presentImage(nextDrawingData)
			} else ExperimentState.complete.value = true
		  }
		}
		loadingProcess.finish()
		ExperimentState.onBreak.not().whenTrueOnce { trial.log += "trial start" }
	  }
	}
	presentImage(DrawingData(imIterator.next(), training = true))
  }
}





