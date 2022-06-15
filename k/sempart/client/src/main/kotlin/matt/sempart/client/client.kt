package matt.sempart.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.ButtonType
import matt.kjs.Path
import matt.kjs.css.Margin.auto
import matt.kjs.css.TextAlign.center
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.elements.appendWrapper
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.img
import matt.kjs.elements.loadingText
import matt.kjs.elements.p
import matt.kjs.node.LoadingProcess
import matt.kjs.req.post
import matt.kjs.setOnClick
import matt.kjs.setOnMouseMove
import matt.sempart.ExperimentData
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingData
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.ExperimentPhase.Instructions
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.state.ExperimentState.begun
import matt.sempart.client.state.ExperimentState.complete
import matt.sempart.client.state.ExperimentState.lastInteract
import matt.sempart.client.state.ExperimentState.onBreak
import matt.sempart.client.state.ExperimentState.working
import matt.sempart.client.state.MyResizeLeft
import matt.sempart.client.state.Participant.pid
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.currentLeft
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.state.pixelIndexIn
import matt.sempart.client.trialdiv.div
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.MouseEvent
import kotlin.js.Date

private val images = ORIG_DRAWING_IMS.shuffled()

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  document.body!!.style.apply {
	background = "black"
	color = "white"
  }
  document.body!!.div {
	onlyShowIn(Instructions)
	sty.textAlign = center
	img {    //			src = "https://www.wolframcloud.com/obj/mjgroth/folder-1/Bd_Jul2018_M_Face_PO1_All.png"
	  src = "data/Bd_Jul2018_M_Face_PO1_All.png"
	}
	p {
	  sty {
		width = WIDTH.px
		margin = auto
	  }
	  innerHTML = """
			Please select the label that you think best matches each segment for
        each
        image. You may select segments either by clicking or by iterating through them with the "next" and
        "previous" buttons. All segments must be given a label before advancing to the next image.
		  """.trimIndent()
	}
	button {
	  sty.margin = auto
	  innerHTML = "Begin Experiment"
	  setOnClick { begun = true }
	}
  }
  val theImg = document.body!!.img { hidden = true }

  document.body!!.div {
	onlyShowIn(Resize)
	p {
	  sty.margin = auto
	  innerHTML =
		"Your window is too small. Please enlarge the browser window so it is at least matt.sempart.client.const.getWidth=900 by matt.sempart.client.const.getHeight=750."
	}
  }
  val loadingDiv = document.body!!.loadingText("Loading") {
	onlyShowIn(Loading)
  }

  fun <R> inLoadingScreen(desc: String, op: ()->R): R = loadingDiv.withLoadingMessage(desc) {
	working = true
	val r = op()
	working = false
	r
  }

  fun startLoadingScreen(desc: String): LoadingProcess {
	working = true
	return LoadingProcess(loadingDiv, desc).apply { start() }
  }

  fun LoadingProcess.finishLoadingScreen() {
	working = true
	finish()
  }



  document.body!!.div {
	onlyShowIn(Complete)
	innerHTML =
	  "Experiment complete. Thank you! Please click this link. It will confirm you have completed the study with Prolific so that you can be paid: <a href=\"https://app.prolific.co/submissions/complete?cc=92B81EA2\">Click here to confirm your completion of this study with Prolific</a>"
  }
  document.body!!.div {
	onlyShowIn(Break)
	sty.textAlign = center
	p {
	  sty {
		width = WIDTH.px
		margin = auto
		innerHTML = "You may take a break and continue when you are ready."
		button {
		  type = ButtonType.button.realValue
		  id = "continueButton"
		  sty.margin = auto
		  innerHTML = "Continue"
		}
	  }
	}
  }
  val continueButton = document.getElementById("continueButton") as HTMLButtonElement
  document.body!!.div {
	onlyShowIn(Inactive)
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }
  var preloadedDrawingData: DrawingData? = null
  var imI = 0
  fun presentImage(im: String) {
	val drawingData = preloadedDrawingData ?: DrawingData(im, theImg)
	val loadingProcess = startLoadingScreen("downloading image data")
	drawingData.whenReady {
	  val drawingTrial = drawingData.trial!!
	  val trialDiv = drawingTrial.div
	  document.body!!.appendWrapper(trialDiv)
	  fun eventToSeg(e: MouseEvent) = e.pixelIndexIn(trialDiv.mainCanvas)?.let { drawingTrial.segmentOf(it) }
	  trialDiv.eventCanvasIDK.setOnMouseMove {
		lastInteract = Date.now()
		drawingTrial.hover(eventToSeg(it))
	  }
	  trialDiv.eventCanvasIDK.onclick = drawingTrial.interaction("click") {
		drawingTrial.select(eventToSeg(it))
	  }
	  drawingTrial.log.add(Date.now().toLong() to "trial start")
	  fun submit(f: ()->Unit) {
		drawingTrial.log.add(Date.now().toLong() to "submit")
		post(
		  Path("send?PROLIFIC_PID=$pid"), ExperimentData(
			responses = drawingTrial.segments.associate { it.id to it.response!! },            /*"image" to im,*/
			trialLog = drawingTrial.log
		  ), f
		)
	  }
	  if (imI < images.size - 1) {
		preloadedDrawingData = DrawingData(images[imI + 1], theImg)
	  }
	  trialDiv.nextImageButton.onclick = drawingTrial.interaction("next image button clicked") {
		if (window.confirm("Are you sure you are ready to proceed? You cannot go back to this image.")) {
		  trialDiv.nextImageButton.disabled = true
		  submit {
			println("in submit op")
			trialDiv.nextImageButton.disabled = true
			imI++
			println("imI is now $imI (images.size=${images.size})")
			if (imI < images.size) {
			  if (imI%PARAMS.breakInterval == 0) {
				onBreak = true
				continueButton.onclick = drawingTrial.interaction("continueButton clicked") {
				  onBreak = false
				  presentImage(images[imI])
				}
			  } else presentImage(images[imI])
			} else complete = true
		  }
		}
	  }
	  loadingProcess.finishLoadingScreen()
	}
  }
  presentImage(images[imI])
  val fireResizeEvent = { MyResizeLeft.dispatchToAllHTML(currentLeft()) }
  window.addEventListener("resize", {
	fireResizeEvent()
	PhaseChange.dispatchToAllHTML(ExperimentPhase.determine())
  })
  fireResizeEvent()
}





