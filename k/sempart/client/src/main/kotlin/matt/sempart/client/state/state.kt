package matt.sempart.client.state

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.ButtonType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import matt.kjs.Loop
import matt.kjs.Path
import matt.kjs.allHTMLElementsRecursive
import matt.kjs.css.Display.InlineBlock
import matt.kjs.css.FontStyle.italic
import matt.kjs.css.FontWeight.bold
import matt.kjs.css.Position.absolute
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.br
import matt.kjs.elements.button
import matt.kjs.elements.canvas
import matt.kjs.elements.div
import matt.kjs.elements.p
import matt.kjs.first
import matt.kjs.firstBackwards
import matt.kjs.img.context2D
import matt.kjs.img.getPixels
import matt.kjs.req.get
import matt.kjs.setOnClick
import matt.kjs.setOnLoad
import matt.kjs.srcAsPath
import matt.klib.todo
import matt.sempart.client.const.DATA_FOLDER
import matt.sempart.client.const.HALF_WIDTH
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.LABELS
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingTrial.Segment
import matt.sempart.client.state.ExperimentPhase.Trial
import matt.sempart.client.sty.box
import matt.sempart.client.sty.boxButton
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.ImageData
import org.w3c.dom.events.EventTarget
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Date

object Participant {
  val pid = URLSearchParams(window.location.href.substringAfter("?")).get("PROLIFIC_PID")
}

object ExperimentState {
  var begun = false
  var lastInteract = Date.now()
  var onBreak = false
  var complete = false
}

enum class ExperimentPhase {
  Instructions,
  Trial,
  Break,
  Complete,
  Inactive,
  Resize,
  Loading
}

abstract class EventDispatcher<T>(type: String? = null) {
  val type: String = type ?: this::class.simpleName!!
  fun dispatchToAllHTML(t: T) {
	val e = CustomEvent(type, object: CustomEventInit {
	  override var detail: Any? = t
	})
	document.allHTMLElementsRecursive().forEach { it.dispatchEvent(e) }
  }
}

fun <T> EventTarget.listen(d: EventDispatcher<T>, op: (T)->Unit) {
  addEventListener(d.type, {
	@Suppress("UNCHECKED_CAST")
	op((it as CustomEvent).detail as T)
  })
}

object PhaseChange: EventDispatcher<ExperimentPhase>()
object MyResizeLeft: EventDispatcher<Int>()

fun HTMLElement.onlyShowIn(phase: ExperimentPhase) {
  hidden = true
  listen(PhaseChange) {
	hidden = it != phase
  }
}

fun currentLeft() = (window.innerWidth/2) - HALF_WIDTH
fun HTMLElement.onMyResizeLeft(onLeft: (Int)->Unit) {
  onLeft(currentLeft())
  listen(MyResizeLeft) {
	onLeft(it)
  }
}


class DrawingTrial(
  val imString: String,
  val imElement: HTMLImageElement
) {

  val segmentsWithResponse get() = segments.filter { it.response != null }
  val completionFraction get() = "${segmentsWithResponse.size}/${segments.size}"
  val isFinished get() = segments.all { it.response != null }
  val isNotFinished get() = !isFinished

  val log = mutableListOf<Pair<Long, String>>()
  var selectedSeg: Segment? = null
  var hoveredSeg: Segment? = null

  override fun toString() = "matt.kjs.idk.Drawing $imString"

  val loadDiv = div {
	todo("loadDiv is not ideal")
	id = "loadDiv"
	hidden = true
  }

  var loadedImage = false
  var loadedIms = 0

  private val _segments = mutableListOf<Segment>()
  val segments: List<Segment> get() = _segments.sortedBy { it.cycleIndex }
  private lateinit var segCycle: ListIterator<Segment>

  var finishedProcessesingResp = false

  init {
	get(
	  DATA_FOLDER + "segment_data2" + "${imString}.json"
	) { resp ->
	  Json
		.decodeFromString<Map<String, List<List<Boolean>>>>(resp)
		.entries
		.let {
		  if (PARAMS.randomSegmentOrder) it.shuffled() else it
		}.forEachIndexed { index, entry ->
		  val ims = (1..5).map {
			(document.createElement("img") as HTMLImageElement).also {
			  loadDiv.appendChild(it)
			  it.hidden = true
			  it.setOnLoad { loadedIms++ }
			}
		  }
		  val (highlightIm, selectIm, labelledIm, selectLabeledIm, hiLabeledIm) = ims

		  val segID = entry.key


		  val imFileName = Path("${imString}_L${segID}.png")

		  highlightIm.srcAsPath = DATA_FOLDER + "segment_highlighted" + imFileName
		  selectIm.srcAsPath = DATA_FOLDER + "segment_selected" + imFileName
		  labelledIm.srcAsPath = DATA_FOLDER + "segment_labelled" + imFileName
		  selectLabeledIm.srcAsPath = DATA_FOLDER + "segment_selected_labeled" + imFileName
		  hiLabeledIm.srcAsPath = DATA_FOLDER + "segment_hi_labeled" + imFileName

		  _segments += Segment(
			id = segID,
			pixels = entry.value,
			highlightIm = highlightIm,
			selectIm = selectIm,
			labelledIm = labelledIm,
			selectLabeledIm = selectLabeledIm,
			hiLabeledIm = hiLabeledIm,
			cycleIndex = index
		  )
		}
	  segCycle = Loop(segments).iterator()
	  finishedProcessesingResp = true
	}
	imElement.setOnLoad {
	  loadedImage = true
	}
	imElement.setAttribute("src", "data/all/${imString}_All.png")
  }

  fun ready(): Boolean {
	return this.loadedImage && this.loadedIms == this.segments.size*5 && finishedProcessesingResp
  }

  fun cleanup() {
	this.loadDiv.remove()
  }


  inner class Segment(
	val id: String,
	val pixels: List<List<Boolean>>,
	val highlightIm: HTMLImageElement,
	val selectIm: HTMLImageElement,
	val labelledIm: HTMLImageElement,
	val selectLabeledIm: HTMLImageElement,
	val hiLabeledIm: HTMLImageElement,
	val cycleIndex: Int
  ) {
	var response: String? = null
	val hasResponse get() = response != null
	val hasNoResponse get() = response == null
	override fun toString() = "Segment $id of ${this@DrawingTrial}"
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
	lateinit var labelledCanvas: HTMLCanvasElement
  }

  lateinit var controlsDiv: HTMLDivElement
  lateinit var stackDiv: HTMLDivElement
  val allButtons = mutableListOf<HTMLButtonElement>()
  lateinit var completionP: HTMLParagraphElement
  val div by lazy { trialDiv() }
  val canvases = mutableListOf<HTMLCanvasElement>()
  val ctxs get() = (canvases - canvases.last()).map { it.context2D }
  lateinit var nextImageButton: HTMLButtonElement

  lateinit var nextUnlabeledSegmentButton: HTMLButtonElement
  lateinit var previousUnlabeledSegmentButton: HTMLButtonElement
  lateinit var previousSegmentButton: HTMLButtonElement
  lateinit var nextSegmentButton: HTMLButtonElement

  lateinit var labelsDiv: HTMLDivElement

  fun Segment.showAsLabeled() {
	val shouldBeImageData: ImageData = selectLabeledPixels
	ctxs[2].putImageData(shouldBeImageData, 0.0, 0.0)
  }

  fun switchSegment(next: Boolean, unlabelled: Boolean) {
	select(when {
	  isFinished && unlabelled -> null
	  selectedSeg == null      -> when {
		next -> segments.first()
		else -> segments.last()
	  }

	  next                     -> segCycle.first { !unlabelled || it.hasNoResponse }
	  else                     -> segCycle.firstBackwards { !unlabelled || it.hasNoResponse }
	})
  }

  fun nextSeg() {
	switchSegment(next = true, unlabelled = true)
  }

  fun select(seg: Segment?) {
	println("selecting ${seg}")
	selectedSeg = seg
	if (seg == null) {
	  log.add(Date.now().toLong() to "unselected segment")
	  labelsDiv.hidden = true
	  canvases[2].hidden = true
	} else {
	  log.add(Date.now().toLong() to "selected $seg")
	  if (seg.hasResponse) {
		seg.showAsLabeled()
		allButtons.forEach { bb ->
		  bb.disabled = seg.response == bb.innerHTML
		}
	  } else {
		val shouldBeImageData = seg.selectPixels
		ctxs[2].putImageData(shouldBeImageData, 0.0, 0.0)
		allButtons.forEach { bb ->
		  bb.disabled = false
		}
	  }
	  labelsDiv.hidden = false
	  canvases[2].hidden = false
	}
  }

  fun Segment?.selectThis() = select(this)

  fun hover(seg: Segment?) {
	if (seg == hoveredSeg) return
	console.log("hovered $seg")
	hoveredSeg = seg
	canvases[1].hidden = hoveredSeg == null
	if (hoveredSeg != null) ctxs[1].putImageData(
	  if (seg!!.hasResponse) seg.hiLabeledPixels else seg.highlightPixels,
	  0.0, 0.0
	)
  }

  fun Segment?.hoverThis() = hover(this)


}

private fun DrawingTrial.trialDiv(): HTMLDivElement = div {
  require(ready())
  onlyShowIn(Trial)
  onMyResizeLeft {
	style.marginLeft = it.toString() + "px"
  }
  stackDiv = div {
	sty.display = InlineBlock
	canvases += (0..3).map {
	  canvas {
		width = WIDTH
		height = HEIGHT
		sty {
		  position = absolute
		  zIndex = it
		  top = 0.px
		}
		onMyResizeLeft {
		  style.left = it.toString() + "px"
		}
		if (it > 1) {
		  sty.zIndex = it + segments.size
		}
	  }
	}
	canvases[0].context2D.drawImage(imElement, 0.0, 0.0)


	segments.forEach { theSeg: Segment ->
	  val zIdx = theSeg.cycleIndex + 1
	  insertBefore(
		canvas {
		  width = WIDTH
		  height = HEIGHT
		  hidden = true
		  sty {
			position = absolute
			top = 0.px
			zIndex = zIdx
		  }
		  context2D.drawImage(theSeg.labelledIm, 0.0, 0.0)
		  onMyResizeLeft {
			sty.left = it.px
		  }
		  println("setting labelledCanvas of ${theSeg}")
		  theSeg.labelledCanvas = this
		},
		children[zIdx]
	  )
	}
  }
  controlsDiv = div {
	sty {
	  display = InlineBlock
	  position = absolute
	}
	onMyResizeLeft {
	  style.left = (it + WIDTH).toString() + "px"
	}
	labelsDiv = div {
	  hidden = true
	  sty.box()
	  (LABELS.shuffled() + "Something else" + "I don't know").forEach { l ->

		allButtons.add(button {
		  disabled = false
		  innerHTML = l
		  sty {
			boxButton()
			fontStyle = italic
		  }
		  setOnClick {
			ExperimentState.lastInteract = Date.now()
			log.add(Date.now().toLong() to "selected label: $l")

			println("getting labelledCanvas of ${selectedSeg}")
			selectedSeg!!.labelledCanvas.hidden = false

			val hadResponse = selectedSeg!!.response != null
			selectedSeg!!.response = l
			completionP.innerHTML =
			  "${segmentsWithResponse}/${segments.size} segments labelled"

			allButtons.forEach { bb ->
			  bb.disabled = bb.innerHTML == l
			}

			nextImageButton.disabled = isNotFinished
			nextUnlabeledSegmentButton.disabled = isFinished
			previousUnlabeledSegmentButton.disabled = isFinished
			if (!hadResponse) {
			  selectedSeg!!.showAsLabeled()
			  nextSeg()
			}
		  }
		})
		br
	  }
	}
	br /*is this enough or do I have to call the function?*/
	br

	div {
	  sty.box()
	  if (!PARAMS.removeNpButtonsKeepUnlabelledNpButtons) {
		div {
		  previousSegmentButton = button {
			type = ButtonType.button.realValue
			sty.boxButton()
			innerHTML = "Previous Segment"
			setOnClick {
			  console.log("previous segment button clicked")
			  ExperimentState.lastInteract = Date.now()
			  disabled = true
			  switchSegment(next = false, unlabelled = false)
			  disabled = false
			}
		  }
		  br {
			id = "previousSegmentButtonBR"
		  }
		  nextSegmentButton = button {
			type = ButtonType.button.realValue
			sty.boxButton()
			innerHTML = "Next Segment"
			setOnClick {
			  println("nextSegmentButton.onclick")
			  console.log("next segment button clicked")
			  ExperimentState.lastInteract = Date.now()
			  disabled = true
			  switchSegment(next = true, unlabelled = false)
			  disabled = false
			}
		  }
		  br {
			id = "nextSegmentButtonBR"
		  }
		}
	  }
	  val unlabelledString = if (PARAMS.removeNpButtonsKeepUnlabelledNpButtons) " Unlabeled" else ""
	  previousUnlabeledSegmentButton = button {
		disabled = false
		type = ButtonType.button.realValue
		sty.boxButton()
		innerHTML = "Previous$unlabelledString Segment"
		setOnClick {
		  console.log("previous unlabeled segment button clicked")
		  ExperimentState.lastInteract = Date.now()
		  previousUnlabeledSegmentButton.disabled = true
		  switchSegment(next = false, unlabelled = true)
		  previousUnlabeledSegmentButton.disabled = false
		}
	  }
	  br
	  nextUnlabeledSegmentButton = button {
		disabled = false
		type = ButtonType.button.realValue
		sty.boxButton()
		innerHTML = "Next$unlabelledString Segment"
		setOnClick {
		  console.log("next unlabeled segment button clicked")
		  ExperimentState.lastInteract = Date.now()
		  nextUnlabeledSegmentButton.disabled = true
		  switchSegment(next = true, unlabelled = true)
		  nextUnlabeledSegmentButton.disabled = false
		}
	  }
	}
	br
	br
	completionP = p {
	  innerHTML = "$completionFraction segments labelled"
	}
	nextImageButton = button {
	  disabled = true
	  type = ButtonType.button.realValue
	  sty {
		fontWeight = bold
		boxButton()
		innerHTML = "Submit Responses and Show Next Image"
	  }
	}
  }
}
