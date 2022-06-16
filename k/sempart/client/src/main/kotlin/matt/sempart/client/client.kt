package matt.sempart.client

import kotlinx.browser.document
import kotlinx.html.dom.append
import kotlinx.html.js.style
import matt.kjs.Path
import matt.kjs.appendChilds
import matt.kjs.css.Color.black
import matt.kjs.css.Color.white
import matt.kjs.css.Position.absolute
import matt.kjs.css.percent
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.elements.HTMLElementWrapper
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
import matt.sempart.client.scaleDiv.scaleDiv
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentPhase.Scaling
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.Participant
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.trialdiv.div
import org.w3c.dom.HTMLElement

//val mainDivClass = "maindiv"

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  val headStyleElement = document.head!!.append.style {}
  headStyleElement.apply {
	type = "text/css"
	this.setAttribute("rel", "stylesheet")
  }
  document.body!!.sty {
	background = black
	color = white
  }

  val divs =
	listOf(scaleDiv, instructionsVidDiv, instructionsDiv, resizeDiv, loadingDiv, completeDiv, breakDiv, inactiveDiv)

  val defaultScale = "1.0"

  document.body!!.appendChilds(
	input {
	  onlyShowIn(Scaling)
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
		//		transform = Tra
	  }
	  oninput = {
		//		headStyleElement.innerHTML = ".$mainDivClass {transform: scale(${value});}"
		//		divs.forEach {
		//
		////		  it.sty.transform = "scale(${value})"
		//		}
		//		document.body!!.sty.transform = "scale(${value})"
		divs.forEach {
		  when (it) {
			is HTMLElement           -> it.sty.modifyTransform {
			  scale = value.toDouble()
			}
			is HTMLElementWrapper<*> -> it.element.sty.modifyTransform {
			  scale = value.toDouble()
			}
		  }
		}
		Unit
	  }
	},
	*divs.toTypedArray()
  )


  /*neccesary so first trialDiv doesnt get NPE, or if slider is never moved*/
  divs.forEach {
	when (it) {
	  is HTMLElement           -> it.sty.modifyTransform {
		scale = defaultScale.toDouble()
	  }

	  is HTMLElementWrapper<*> -> it.element.sty.modifyTransform {
		scale = defaultScale.toDouble()
	  }
	}
  }

  val images = listOf(TRAIN_IM) + ORIG_DRAWING_IMS.shuffled()
  val imIterator = images.withIndex().toList().listIterator()

  /*need scale before creating elements*/
  PhaseChange.afterEndOfNext(Scaling) {
	fun presentImage(drawingData: DrawingData, training: Boolean = false) {
	  val loadingProcess = DrawingLoadingProcess("downloading image data")
	  drawingData.whenReady {
		val trial = drawingData.trial!!
		trial.div.element.sty.modifyTransform {
		  scale = scaleDiv.sty.transform.scale
		}
//		trial.div.element.sty.transform = Transform().apply {
//		  val scaleDivTransform = scaleDiv.sty.transform
//		  println("scaleDivTransform:")
////		  scaleDivTransform.map.forEach {
////			tab("k=${it.key}")
////			it.value.forEach {
////			  tab("\t$it")
////			}
////		  }
//
//		  map["scale"] = scaleDiv.sty.transform.map["scale"]!!
//		}

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


}





