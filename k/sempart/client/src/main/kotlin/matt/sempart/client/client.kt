package matt.sempart.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.ButtonType
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.canvas
import kotlinx.html.div
import kotlinx.html.hidden
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import matt.kjs.css.BorderStyle.solid
import matt.kjs.css.BorderWidth.thin
import matt.kjs.css.Display.InlineBlock
import matt.kjs.css.FontWeight.bold
import matt.kjs.css.Margin.auto
import matt.kjs.css.Position.absolute
import matt.kjs.css.TextAlign.center
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.defaultMain
import matt.kjs.div
import matt.kjs.img.getPixels
import matt.kjs.setOnClick
import matt.kjs.setOnLoad
import matt.kjs.setOnMouseMove
import matt.sempart.ExperimentData
import matt.sempart.client.Drawing.Segment
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Date

const val WIDTH = 600
const val HEIGHT = 700
const val HALF_WIDTH = WIDTH/2

const val BREAK_INTERVAL = 2
const val REMOVE_NP_BUTTONS_KEEP_UNLABELLED_NP_BUTTONS = true
const val RANDOMIZE_SEGMENT_ORDER = true


@Suppress("unused")
const val MINUTE_MS = 60*1000
const val HOUR_MS = 3600*1000
const val IDLE_THRESHOLD = HOUR_MS /*JUST FOR DEV. USE MINUTE FOR FINAL?*/

fun main() = defaultMain {
  document.head!!.title = "Semantic Segmentation"
  document.body!!.style.apply {
	background = "black"
	color = "white"
  }


  val instructionsDiv = div {
	sty.textAlign = center
	img {
	  //			src = "https://www.wolframcloud.com/obj/mjgroth/folder-1/Bd_Jul2018_M_Face_PO1_All.png"
	  src = "data/Bd_Jul2018_M_Face_PO1_All.png"
	}
	p {
	  sty {
		width = WIDTH.px
		margin = auto
	  }
	  +"""
			Please select the label that you think best matches each segment for
        each
        image. You may select segments either by clicking or by iterating through them with the "next" and
        "previous" buttons. All segments must be given a label before advancing to the next image.
		  """.trimIndent()
	}
	button {
	  id = "beginButton"
	  sty.margin = auto
	  +"Begin Experiment"
	}
  }

  val mainDiv = div {
	hidden = true
	img(classes = "orig") {
	  hidden = true
	}
	div {
	  id = "stack"
	  sty.display = InlineBlock
	  (1..4).forEach {
		canvas(classes = "canvas$it") {
		  id = "canvas$it"
		  width = WIDTH.toString()
		  height = HEIGHT.toString()
		  sty.position = absolute
		  sty.zIndex = it - 1
		  sty.top = 0.px
		}
	  }
	}
	div {
	  id = "controlsDiv"
	  sty {
		display = InlineBlock
		position = absolute
	  }
	  div {
		id = "labels"
		sty {
		  borderStyle = solid
		  borderWidth = thin
		}
	  }
	  br /*is this enough or do I have to call the function?*/
	  br
	  div {
		sty {
		  borderStyle = solid
		  borderWidth = thin
		}
		div {
		  id = "nextPrevButtons"
		  button {
			type = ButtonType.button
			id = "previousSegmentButton"
			sty {
			  marginBottom = 5.px
			  marginTop = 5.px
			  marginLeft = 5.px
			}
			+"Previous Segment"
		  }
		  br {
			id = "previousSegmentButtonBR"
		  }
		  button {
			type = ButtonType.button
			id = "nextSegmentButton"
			sty {
			  marginBottom = 5.px
			  marginTop = 5.px
			  marginLeft = 5.px
			}
			+"Next Segment"
		  }
		  br {
			id = "nextSegmentButtonBR"
		  }
		}
		button {
		  type = ButtonType.button
		  id = "previousUnlabeledSegmentButton"
		  sty {
			marginBottom = 5.px
			marginTop = 5.px
			marginLeft = 5.px
		  }
		  +"Previous Unlabeled Segment"
		}
		br
		button {
		  type = ButtonType.button
		  id = "nextUnlabeledSegmentButton"
		  sty {
			marginBottom = 5.px
			marginTop = 5.px
			marginLeft = 5.px
		  }
		  +"Next Unlabeled Segment"
		}
	  }
	  br
	  br
	  p {
		id = "completionP"
	  }
	  button {
		type = ButtonType.button
		id = "nextImageButton"
		sty {
		  fontWeight = bold
		  marginBottom = 5.px
		  marginTop = 5.px
		  marginLeft = 5.px
		  +"Submit Responses and Show Next Image"
		}
	  }
	}
  }

  val img = document.querySelector(".orig") as HTMLImageElement
  val stackDiv = document.getElementById("stack") as HTMLDivElement
  val controlsDiv = document.getElementById("controlsDiv") as HTMLDivElement
  val labelsDiv = document.getElementById("labels") as HTMLDivElement
  val nextImageButton = document.getElementById("nextImageButton") as HTMLButtonElement
  val nextSegmentButton = document.getElementById("nextSegmentButton") as HTMLButtonElement
  val previousSegmentButton = document.getElementById("previousSegmentButton") as HTMLButtonElement
  val nextUnlabeledSegmentButton = document.getElementById("nextUnlabeledSegmentButton") as HTMLButtonElement
  val previousUnlabeledSegmentButton =
	document.getElementById("previousUnlabeledSegmentButton") as HTMLButtonElement

  val resizeDiv = div {
	hidden = true
	p {
	  sty.margin = auto
	  +"Your window is too small. Please enlarge the browser window so it is at least width=900 by height=750."
	}
  }

  val loadingDiv = div {
	hidden = true
	+"Loading"
  }

  val breakDiv = div {
	hidden = true
	sty.textAlign = center
	p {
	  sty {
		width = WIDTH.px
		margin = auto
		+"You may take a break and continue when you are ready."
		button {
		  type = ButtonType.button
		  id = "continueButton"
		  sty.margin = auto
		  +"Continue"
		}
	  }
	}
  }
  val continueButton = document.getElementById("continueButton") as HTMLButtonElement

  val inactiveDiv = div {
	hidden = true
	+"Sorry, you have been inactive for too long and the experiment has been cancelled."
  }

  val realImages =
	mutableListOf("Face/Bd_Jul2018_M_Face_PO1", "Face/Ba_Jan2018_F_Face_FU3", "Face/Lo_Jan2018_M_Face_FU1")

  val images = realImages.shuffled()

  val labels = listOf(
	"eye",
	"mouth",
	"ear",
	"nose"
  )


  var responses = mutableMapOf<String, String>()
  var selectedSeg: Segment? = null
  val completionP = document.getElementById("completionP")
  var trialLog: MutableList<Pair<Long, String>>? = null
  var onBreak = false
  val pid = URLSearchParams(window.location.href.substringAfter("?")).get("PROLIFIC_PID")


  if (REMOVE_NP_BUTTONS_KEEP_UNLABELLED_NP_BUTTONS) {
	val nextPrevButtonsDiv = document.getElementById("nextPrevButtons") as HTMLDivElement
	nextPrevButtonsDiv.hidden = true
	nextPrevButtonsDiv.style.display = "none"
	nextUnlabeledSegmentButton.innerHTML = nextSegmentButton.innerHTML
	previousUnlabeledSegmentButton.innerHTML = previousSegmentButton.innerHTML
  }


  val canvas1 = document.querySelector(".canvas1") as HTMLCanvasElement
  val ctx1 = canvas1.getContext("2d") as CanvasRenderingContext2D
  val canvas2 = document.querySelector(".canvas2") as HTMLCanvasElement
  val ctx2 = canvas2.getContext("2d") as CanvasRenderingContext2D
  val canvas3 = document.querySelector(".canvas3") as HTMLCanvasElement
  val ctx3 = canvas3.getContext("2d") as CanvasRenderingContext2D
  val canvas4 = document.querySelector(".canvas4") as HTMLCanvasElement


  var lastInteract = Date.now()

  var debugTic: Double?

  val allButtons = mutableListOf<HTMLButtonElement>()


  var preloadedDrawingData: Drawing? = null

  var showThisAsLabeledFunc: (()->Unit)? = null
  var nextSegFunc: (()->Unit)? = null

  var currentNSegments = 0

  var begun = false
  (document.getElementById("beginButton") as HTMLButtonElement).setOnClick {
	begun = true
  }


  val labelledCanvases = mutableListOf<HTMLCanvasElement>()

  val shuffledLabels = labels.shuffled().toMutableList()
  shuffledLabels.add("Something else")
  shuffledLabels.add("I don't know")
  console.log("here5")
  shuffledLabels.forEach { l ->
	val b = document.createElement("button") as HTMLButtonElement
	b.innerHTML = l
	b.style.marginBottom = "5px"
	b.style.marginTop = "5px"
	b.style.marginLeft = "5px"
	b.style.fontStyle = "italic"
	b.onclick = {
	  println("b.onclick")
	  lastInteract = Date.now()
	  trialLog!!.add(Date.now().toLong() to "selected label: ${b.innerHTML}")

	  labelledCanvases[selectedSeg!!.id.toInt() - 1].hidden = false


	  val hadResponse = selectedSeg!!.id in responses.keys
	  responses[selectedSeg!!.id] = b.innerHTML
	  completionP!!.innerHTML = "${responses.keys.size}/${currentNSegments} segments labelled"
	  allButtons.forEach { bb ->
		bb.disabled = bb === b
	  }
	  /*submitSegmentButton*/

	  val finished = (responses.keys.size == currentNSegments)
	  nextImageButton.disabled = !finished
	  nextUnlabeledSegmentButton.disabled = finished
	  previousUnlabeledSegmentButton.disabled = finished
	  if (!hadResponse) {
		showThisAsLabeledFunc!!()
		nextSegFunc!!()
	  }
	}
	allButtons.add(b)
	labelsDiv.appendChild(b)
	labelsDiv.appendChild(document.createElement("br"))
  }

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

  fun finishedWorking() {
	working = false
	console.log("finished working")
  }

  console.log("here6")

  var imI = 0

  fun presentImage(im: String) {
	nextImageButton.disabled = true
	nextUnlabeledSegmentButton.disabled = false
	previousUnlabeledSegmentButton.disabled = false
	allButtons.forEach { bb ->
	  bb.disabled = false
	}
	labelsDiv.hidden = true

	workingOn("downloading image data")

	trialLog = mutableListOf()
	responses = mutableMapOf()

	val drawingData = preloadedDrawingData ?: Drawing(im, img)

	completionP!!.innerHTML = "${responses.keys.size}/${drawingData.segments.size} segments labelled"


	var theInterval: Int? = null

	theInterval = window.setInterval({
	  if (drawingData.ready()) {
		currentNSegments = drawingData.segments.size
		workingOn("processing image data")

		ctx1.drawImage(img, 0.0, 0.0)

		@Suppress("UNUSED_VARIABLE") val origImageData =
		  ctx1.getImageData(0.0, 0.0, WIDTH.toDouble(), HEIGHT.toDouble())

		val segmentsToCycleIndex: Map<Segment, Int> = drawingData.segments.let {
		  if (RANDOMIZE_SEGMENT_ORDER) it.shuffled() else it
		}.withIndex().associate { it.value to it.index }

		val cycleIndexToSegments = segmentsToCycleIndex.map { it.value to it.key }.toMap()

		labelledCanvases.forEach { lc ->
		  stackDiv.removeChild(lc)
		}
		labelledCanvases.clear()
		(0 until drawingData.segments.size).forEach { i ->
		  val lc = document.createElement("canvas") as HTMLCanvasElement
		  lc.width = WIDTH
		  lc.height = HEIGHT
		  lc.style.position = "absolute"
		  lc.style.top = "0px"
		  lc.style.zIndex = (i + 1).toString()
		  lc.hidden = true
		  val lctx = lc.getContext("2d") as CanvasRenderingContext2D
		  lctx.drawImage(drawingData.labelledIms[i], 0.0, 0.0)
		  stackDiv.insertBefore(lc, stackDiv.children[i + 1])
		  labelledCanvases.add(lc)
		}

		canvas2.style.zIndex = (1 + drawingData.segments.size).toString()
		canvas3.style.zIndex = (2 + drawingData.segments.size).toString()
		canvas4.style.zIndex = (3 + drawingData.segments.size).toString()



		console.log("setting up handlers")

		var lastEvent: Event? = null
		var lastEventWorked: Event? = null
		var lastSelectedSegWorked: Segment? = null


		canvas4.setOnMouseMove { lastEvent = it }

		fun eventToSeg(e: MouseEvent): Segment? {
		  val x = e.clientX - canvas1.offsetLeft
		  val y = e.clientY - canvas1.offsetTop
		  if (x < 0 || y < 0) return null
		  return drawingData.segments.firstOrNull {
			it.pixels[y][x]
		  }
		}

		selectedSeg = null
		var hoveredSeg: Segment? = null
		canvas4.setOnClick { e: Event ->
		  println("canvas4.onclick")
		  lastInteract = Date.now()
		  selectedSeg = eventToSeg(e as MouseEvent)
		}

		canvas3.hidden = true

		fun select(seg: Segment?) {
		  console.log("selected $seg")

		  if (seg == null) {
			trialLog!!.add(Date.now().toLong() to "unselected segment")
			labelsDiv.hidden = true
			canvas3.hidden = true
		  } else {
			trialLog!!.add(Date.now().toLong() to "selected $seg")


			showThisAsLabeledFunc = {
			  val shouldBeImageData: ImageData = seg.selectLabeledPixels
			  ctx3.putImageData(shouldBeImageData, 0.0, 0.0)
			}


			if (seg.id in responses.keys) {

			  showThisAsLabeledFunc!!()

			  allButtons.forEach { bb ->
				bb.disabled = responses[seg.id] === bb.innerHTML
			  }
			} else {

			  val shouldBeImageData = seg.selectPixels
			  ctx3.putImageData(shouldBeImageData, 0.0, 0.0)

			  allButtons.forEach { bb ->
				bb.disabled = false
			  }
			}

			labelsDiv.hidden = false
			canvas3.hidden = false
		  }
		}

		canvas2.hidden = true
		val imageInterval = window.setInterval({
		  if (lastEvent != lastEventWorked || lastSelectedSegWorked != selectedSeg) {
			lastInteract = Date.now()

			lastSelectedSegWorked = selectedSeg

			if (selectedSeg != null) {
			  select(selectedSeg)
			} else {
			  labelsDiv.hidden = true
			}
			if (lastEvent !== lastEventWorked) {
			  lastEventWorked = lastEvent
			  val seg = eventToSeg(lastEvent as MouseEvent)
			  if (seg != hoveredSeg) {
				console.log("hovered $seg")
				hoveredSeg = seg
				if (hoveredSeg == null) {
				  canvas2.hidden = true
				} else {
				  if (seg.toString() in responses.keys) {
					val shouldBeImageData: ImageData = seg!!.hiLabeledPixels
					ctx2.putImageData(shouldBeImageData, 0.0, 0.0)
				  } else {
					val shouldBeImageData: ImageData = seg!!.highlightPixels
					ctx2.putImageData(shouldBeImageData, 0.0, 0.0)
				  }

				  canvas2.hidden = false
				}
			  }
			}
		  }
		}, 25)
		finishedWorking()
		window.clearInterval(theInterval!!)
		trialLog!!.add(Date.now().toLong() to "trial start")

		console.log("started trial")



		fun switchSegment(next: Boolean, unlabelled: Boolean) {
		  console.log("next unlabeled segment")
		  if (responses.size == cycleIndexToSegments.size && unlabelled) {
			select(null)
			return
		  }
		  val default = if (next) cycleIndexToSegments[0] else cycleIndexToSegments[drawingData.segments.size - 1]
		  println("default=${default}")
		  do {
			selectedSeg = if (selectedSeg == null) default
			else cycleIndexToSegments[
				segmentsToCycleIndex[selectedSeg!!]!! + (if (next) 1 else -1)
			] ?: default
			println("maybe selectedSegID = $selectedSeg")
		  } while (unlabelled && selectedSeg!!.id in responses.keys)
		  select(selectedSeg)
		}
		nextSegFunc = { switchSegment(next = true, unlabelled = true) }

		nextSegmentButton.setOnClick {
		  println("nextSegmentButton.onclick")
		  console.log("next segment button clicked")
		  lastInteract = Date.now()
		  nextSegmentButton.disabled = true
		  switchSegment(next = true, unlabelled = false)
		  nextSegmentButton.disabled = false
		}
		previousSegmentButton.setOnClick {
		  console.log("previous segment button clicked")
		  lastInteract = Date.now()
		  previousSegmentButton.disabled = true
		  switchSegment(next = false, unlabelled = false)
		  previousSegmentButton.disabled = false
		}
		nextUnlabeledSegmentButton.setOnClick {
		  console.log("next unlabeled segment button clicked")
		  lastInteract = Date.now()
		  nextUnlabeledSegmentButton.disabled = true
		  switchSegment(next = true, unlabelled = true)
		  nextUnlabeledSegmentButton.disabled = false
		}
		previousUnlabeledSegmentButton.setOnClick {
		  console.log("previous unlabeled segment button clicked")
		  lastInteract = Date.now()
		  previousUnlabeledSegmentButton.disabled = true
		  switchSegment(next = false, unlabelled = true)
		  previousUnlabeledSegmentButton.disabled = false
		}

		fun submit(f: ()->Unit) {

		  console.log("submit button clicked")

		  // submitSegmentButton.disabled = true
		  trialLog!!.add(Date.now().toLong() to "submit")

		  drawingData.cleanup()

		  val req = XMLHttpRequest()
		  req.addEventListener("load", {

		  })
		  println("opening post")
		  req.open("POST", "send?PROLIFIC_PID=" + pid, async = false)
		  println("opened post")
		  req.setRequestHeader("Content-Type", "application/json;charset=UTF-8")
		  println("set request header")
		  req.onreadystatechange = {
			println("ready status: ${req.status}")
			/*if (req.status == 200.toShort()) {*/
			if (req.status == 201.toShort()) {
			  f()
			}
		  }
		  println("sending json")
		  req.send(
			Json.encodeToString(
			  ExperimentData(
				responses = responses,
				/*"image" to im,*/
				trialLog = trialLog!!
			  )
			)
		  )
		  println("sent json")

		}

		if (imI < images.size - 1) {
		  preloadedDrawingData = Drawing(images[imI + 1], img)
		}

		nextImageButton.onclick = {

		  console.log("next image button clicked")

		  lastInteract = Date.now()
		  if (window.confirm("Are you sure you are ready to proceed? You cannot go back to this image.")) {
			nextImageButton.disabled = true
			submit {
			  println("in submit op")
			  debugTic = Date.now()
			  window.clearInterval(imageInterval)
			  nextImageButton.disabled = true
			  imI++
			  println("imI is now $imI (images.size=${images.size})")
			  if (imI < images.size) {
				if (imI%BREAK_INTERVAL == 0) {
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
				mainDiv.innerHTML =
				  "Experiment complete. Thank you! Please click this link. It will confirm you have completed the study with Prolific so that you can be paid: <a href=\"https://app.prolific.co/submissions/complete?cc=92B81EA2\">Click here to confirm your completion of this study with Prolific</a>"
			  }
			  val debugToc = Date.now()
			  val computeTime = debugToc - debugTic!!
			  console.log("computeTime:${computeTime/1000.0}s")
			}
		  }

		}

	  }
	  //	  Unit
	}, 10)

  }
  presentImage(images[imI])
  window.setInterval({
	val w = window.innerWidth
	val h = window.innerHeight
	val left = (w/2) - HALF_WIDTH
	mainDiv.style.marginLeft = left.toString() + "px"
	canvas1.style.left = left.toString() + "px"
	canvas2.style.left = left.toString() + "px"
	canvas3.style.left = left.toString() + "px"
	canvas4.style.left = left.toString() + "px"
	controlsDiv.style.left = (left + WIDTH).toString() + "px"
	(0 until labelledCanvases.size).forEach {
	  labelledCanvases[it].style.left = left.toString() + "px"
	}
	instructionsDiv.hidden = begun
	if (begun) {
	  val showDiv = when {
		Date.now() - lastInteract >= IDLE_THRESHOLD -> inactiveDiv
		onBreak                                     -> breakDiv
		w < 1200 || h < 750                         -> resizeDiv
		working                                     -> loadingDiv
		else                                        -> mainDiv
	  }
	  document.body!!.childNodes.asList()
		.filterIsInstance<HTMLDivElement>()
		.forEach {
		  it.hidden = it != showDiv
		}
	}
  }, 100)
}


class Drawing(
  val im: String,
  mainIm: HTMLImageElement
) {

  override fun toString() = "matt.kjs.idk.Drawing $im"

  val loadDiv = div {
	hidden = true
  }
  var highlightIms = mutableListOf<HTMLImageElement>()


  var selectIms = mutableListOf<HTMLImageElement>()
  var labelledIms = mutableListOf<HTMLImageElement>()
  var selectLabeledIms = mutableListOf<HTMLImageElement>()
  var hiLabeledIms = mutableListOf<HTMLImageElement>()

  var loadedImage = false
  var loadedIms = 0

  private val _segments = mutableListOf<Segment>()
  val segments: List<Segment> = _segments

  init {

	val req = XMLHttpRequest()

	req.addEventListener("load", {
	  val resp = req.responseText
	  console.log("resp::class=${resp::class}")
	  Json
		.decodeFromString<Map<String, List<List<Boolean>>>>(resp)
		.entries
		.forEach { entry ->
		  val ims = (1..5).map {
			(document.createElement("img") as HTMLImageElement).also {
			  loadDiv.appendChild(it)
			  it.hidden = true
			  it.setOnLoad { loadedIms++ }
			}
		  }
		  val (highlightIm, selectIm, labelledIm, selectLabeledIm, hiLabeledIm) = ims

		  highlightIms.add(highlightIm)
		  selectIms.add(selectIm)
		  labelledIms.add(labelledIm)
		  selectLabeledIms.add(selectLabeledIm)
		  hiLabeledIms.add(hiLabeledIm)

		  val segID = entry.key

		  highlightIm.src = "data/segment_highlighted/${im}_L${segID}.png"
		  selectIm.src = "data/segment_selected/${im}_L${segID}.png"
		  labelledIm.src = "data/segment_labelled/${im}_L${segID}.png"
		  selectLabeledIm.src = "data/segment_selected_labeled/${im}_L${segID}.png"
		  hiLabeledIm.src = "data/segment_hi_labeled/${im}_L${segID}.png"

		  _segments += Segment(
			id = segID,
			pixels = entry.value,
			highlightIm = highlightIm,
			selectIm = selectIm,
			labelledIm = labelledIm,
			selectLabeledIm = selectLabeledIm,
			hiLabeledIm = hiLabeledIm
		  )
		}
	})
	req.open("GET", "data/segment_data2/${im}.json", async = true)
	req.send()
	mainIm.setOnLoad {
	  loadedImage = true
	}
	mainIm.setAttribute("src", "data/all/${im}_All.png")
  }

  fun cleanup() {
	this.loadDiv.remove()
  }

  fun ready(): Boolean {
	return this.loadedImage && this.loadedIms == this.segments.size*5
  }

  inner class Segment(
	val id: String,
	val pixels: List<List<Boolean>>,
	highlightIm: HTMLImageElement,
	selectIm: HTMLImageElement,
	labelledIm: HTMLImageElement,
	selectLabeledIm: HTMLImageElement,
	hiLabeledIm: HTMLImageElement
  ) {
	override fun toString() = "Segment $id of ${this@Drawing}"
	val highlightPixels by lazy {
	  highlightIm.getPixels(w = WIDTH, h = HEIGHT)
	}
	val selectPixels by lazy {
	  selectIm.getPixels(w = WIDTH, h = HEIGHT)
	}
	val labelledPixels by lazy {
	  labelledIm.getPixels(w = WIDTH, h = HEIGHT)
	}
	val selectLabeledPixels by lazy {
	  selectLabeledIm.getPixels(w = WIDTH, h = HEIGHT)
	}
	val hiLabeledPixels by lazy {
	  hiLabeledIm.getPixels(w = WIDTH, h = HEIGHT)
	}
  }

}