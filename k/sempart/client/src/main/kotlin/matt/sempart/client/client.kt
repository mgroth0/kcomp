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
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.img
import matt.kjs.elements.p
import matt.kjs.img.context2D
import matt.kjs.req.post
import matt.kjs.setOnClick
import matt.kjs.setOnMouseMove
import matt.sempart.ExperimentData
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.ORIG_DRAWING_IMS
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingTrial
import matt.sempart.client.state.DrawingTrial.Segment
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.ExperimentPhase.Instructions
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.state.ExperimentPhase.Trial
import matt.sempart.client.state.ExperimentState.begun
import matt.sempart.client.state.ExperimentState.complete
import matt.sempart.client.state.ExperimentState.lastInteract
import matt.sempart.client.state.ExperimentState.onBreak
import matt.sempart.client.state.MyResizeLeft
import matt.sempart.client.state.Participant.pid
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.currentLeft
import matt.sempart.client.state.onMyResizeLeft
import matt.sempart.client.state.onlyShowIn
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Date

private val images = ORIG_DRAWING_IMS.shuffled()

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  document.body!!.style.apply {
	background = "black"
	color = "white"
  }

  val instructionsDiv = div {
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

  val theImg = img { hidden = true }
  var trialDiv: HTMLDivElement? = null

  val resizeDiv = div {
	onlyShowIn(Resize)
	p {
	  sty.margin = auto
	  innerHTML =
		"Your window is too small. Please enlarge the browser window so it is at least matt.sempart.client.const.getWidth=900 by matt.sempart.client.const.getHeight=750."
	}
  }

  val loadingDiv = div {
	onlyShowIn(Loading)
	innerHTML = "Loading"
  }

  val expCompleteDiv = div {
	onlyShowIn(Complete)
	innerHTML =
	  "Experiment complete. Thank you! Please click this link. It will confirm you have completed the study with Prolific so that you can be paid: <a href=\"https://app.prolific.co/submissions/complete?cc=92B81EA2\">Click here to confirm your completion of this study with Prolific</a>"
  }

  val breakDiv = div {
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

  val inactiveDiv = div {
	onlyShowIn(Inactive)
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }

  var debugTic: Double?

  var preloadedDrawingData: DrawingTrial? = null
  var loadingMessage = ""
  val loadDotN = 1
  var working = false

  window.setInterval({
	if (!loadingDiv.hidden) {
	  var dotString = " "
	  repeat((0..loadDotN).count()) {
		dotString = "$dotString."
	  }
	  loadingDiv.innerHTML = "${loadingMessage}${dotString}"
	}
  }, 100)

  fun workingOn(s: String) {
	loadingMessage = s
	console.log("working on: $s")
	working = true
  }

  var imI = 0

  fun presentImage(im: String) {
	val drawingTrial = preloadedDrawingData ?: DrawingTrial(im, theImg)
	workingOn("downloading image data")
	var theInterval: Int? = null
	theInterval = window.setInterval({
	  if (drawingTrial.ready()) {

		trialDiv?.remove()
		trialDiv = drawingTrial.div

		workingOn("processing image data")

		drawingTrial.ctxs[0].drawImage(theImg, 0.0, 0.0)

		@Suppress("UNUSED_VARIABLE") val origImageData =
		  drawingTrial.ctxs[0].getImageData(0.0, 0.0, WIDTH.toDouble(), HEIGHT.toDouble())


		drawingTrial.labelledCanvases.clear()
		drawingTrial.segments.forEach { theSeg ->
		  val lc = document.createElement("canvas") as HTMLCanvasElement
		  lc.width = WIDTH
		  lc.height = HEIGHT
		  lc.style.position = "absolute"
		  lc.style.top = "0px"
		  val zIndex = theSeg.cycleIndex + 1
		  lc.style.zIndex = zIndex.toString()
		  lc.hidden = true
		  lc.context2D.drawImage(theSeg.labelledIm, 0.0, 0.0)
		  lc.onMyResizeLeft {
			lc.style.left = it.toString() + "px"
		  }
		  drawingTrial.stackDiv.insertBefore(lc, drawingTrial.stackDiv.children[zIndex])
		  drawingTrial.labelledCanvases.add(lc)
		}


		drawingTrial.canvases[1].style.zIndex = (1 + drawingTrial.segments.size).toString()
		drawingTrial.canvases[2].style.zIndex = (2 + drawingTrial.segments.size).toString()
		drawingTrial.canvases[3].style.zIndex = (3 + drawingTrial.segments.size).toString()



		console.log("setting up handlers")

		var lastEvent: Event? = null
		var lastEventWorked: Event? = null
		var lastSelectedSegWorked: Segment? = null



		drawingTrial.canvases[3].setOnMouseMove { lastEvent = it }

		fun eventToSeg(e: MouseEvent): Segment? {
		  val x = e.clientX - drawingTrial.canvases[0].offsetLeft
		  val y = e.clientY - drawingTrial.canvases[0].offsetTop
		  if (x < 0 || y < 0) return null
		  return drawingTrial.segments.firstOrNull {
			it.pixels[y][x]
		  }
		}

		drawingTrial.selectedSeg = null
		drawingTrial.hoveredSeg = null

		drawingTrial.canvases[3].setOnClick { e: Event ->
		  println("canvas4.onclick")
		  lastInteract = Date.now()
		  drawingTrial.selectedSeg = eventToSeg(e as MouseEvent)
		}

		drawingTrial.canvases[2].hidden = true






		drawingTrial.canvases[1].hidden = true

		val imageInterval = window.setInterval({
		  if (lastEvent != lastEventWorked || lastSelectedSegWorked != drawingTrial.selectedSeg) {
			lastInteract = Date.now()
			lastSelectedSegWorked = drawingTrial.selectedSeg
			drawingTrial.labelsDiv.hidden = drawingTrial.selectedSeg == null
			drawingTrial.select(drawingTrial.selectedSeg)
			if (lastEvent != lastEventWorked) {
			  lastEventWorked = lastEvent
			  drawingTrial.hover(eventToSeg(lastEvent as MouseEvent))
			}
		  }
		}, 25)
		working = false
		window.clearInterval(theInterval!!)
		drawingTrial.log.add(Date.now().toLong() to "trial start")






		fun submit(f: ()->Unit) {
		  drawingTrial.log.add(Date.now().toLong() to "submit")
		  drawingTrial.cleanup()
		  post(
			Path("send?PROLIFIC_PID=$pid"), ExperimentData(
			  responses = drawingTrial.segments.associate { it.id to it.response!! },            /*"image" to im,*/
			  trialLog = drawingTrial.log
			), f
		  )
		}

		if (imI < images.size - 1) {
		  preloadedDrawingData = DrawingTrial(images[imI + 1], theImg)
		}

		drawingTrial.nextImageButton.onclick = {

		  console.log("next image button clicked")

		  lastInteract = Date.now()
		  if (window.confirm("Are you sure you are ready to proceed? You cannot go back to this image.")) {
			drawingTrial.nextImageButton.disabled = true
			submit {
			  println("in submit op")
			  debugTic = Date.now()
			  window.clearInterval(imageInterval)
			  drawingTrial.nextImageButton.disabled = true
			  imI++
			  println("imI is now $imI (images.size=${images.size})")
			  if (imI < images.size) {
				if (imI%PARAMS.breakInterval == 0) {
				  onBreak = true
				  println("break started")
				  val tempInterval = window.setInterval({
					lastInteract = Date.now()
				  }, 1000)
				  continueButton.onclick = {
					println("continueButton clicked")
					window.clearInterval(tempInterval)
					lastInteract = Date.now()
					onBreak = false
					println("break ended")
					presentImage(images[imI])
				  }
				} else presentImage(images[imI])

			  } else {
				complete = true
			  }
			  val debugToc = Date.now()
			  val computeTime = debugToc - debugTic!!
			  console.log("computeTime:${computeTime/1000.0}s")
			}
		  }

		}

	  }
	}, 10)

  }
  presentImage(images[imI])

  val fireResizeEvent = {
	MyResizeLeft.dispatchToAllHTML(currentLeft())
	if (window.innerWidth < 1200 || window.innerHeight < 750) {

	}
  }
  window.addEventListener("resize", { fireResizeEvent() })
  fireResizeEvent()

  window.setInterval({
	val w = window.innerWidth
	val h = window.innerHeight
	val phase = when {
	  !begun                                            -> Instructions
	  complete                                          -> Complete
	  onBreak                                           -> Break
	  Date.now() - lastInteract >= PARAMS.idleThreshold -> Inactive
	  w < 1200 || h < 750                               -> Resize
	  working                                           -> Loading
	  else                                              -> Trial
	}
	PhaseChange.dispatchToAllHTML(phase)
  }, 100)
}





