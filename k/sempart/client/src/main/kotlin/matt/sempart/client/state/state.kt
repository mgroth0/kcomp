package matt.sempart.client.state

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import matt.kjs.Loop
import matt.kjs.Path
import matt.kjs.allHTMLElementsRecursive
import matt.kjs.elements.AwesomeElement
import matt.kjs.elements.canvas
import matt.kjs.elements.div
import matt.kjs.elements.img
import matt.kjs.every
import matt.kjs.first
import matt.kjs.firstBackwards
import matt.kjs.img.context2D
import matt.kjs.img.getPixels
import matt.kjs.prop.BindableProperty
import matt.kjs.prop.ReadOnlyBindableProperty
import matt.kjs.req.get
import matt.kjs.setOnLoad
import matt.kjs.srcAsPath
import matt.klib.todo
import matt.sempart.client.const.DATA_FOLDER
import matt.sempart.client.const.HALF_WIDTH
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingData.Segment
import matt.sempart.client.state.ExperimentPhase.Companion.currentPhase
import matt.sempart.client.state.ExperimentState.working
import matt.sempart.client.trialdiv.div
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Date
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

object Participant {
  val pid = URLSearchParams(window.location.href.substringAfter("?")).get("PROLIFIC_PID")
}


object ExperimentState {
  var lastInteract = Date.now()
	.apply {    /*only way to check for lastInteract I think*/    /*I could also check every time lastInteract changes, but that could be even more expensive because of mouse moves?*/
	  every(PARAMS.idleCheckPeriodMS.milliseconds) {
		PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
	  }
	}
  var begun by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
  }
  var onBreak by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
  }
  var complete by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
  }
  var working by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
  }
}


//fun <R> LoadingProcess.finishedLoadingScreen(desc: String? = null, op: ()->R): R {
//  val r = finish(desc) { op() }
//  working = false
//  return r
//}
//
//fun <R> LoadingProcess.finishedLoadingScreen() {
//  finish()
//  working = false
//}

enum class ExperimentPhase {
  Instructions, Trial, Break, Complete, Inactive, Resize, Loading;

  companion object {
	val currentPhase: ReadOnlyBindableProperty<ExperimentPhase> = BindableProperty(determine()).apply {
	  PhaseChange.beforeDispatch {
		if (it.second != value) value = it.second
	  }
	}

	fun determine(): ExperimentPhase {
	  val w = window.innerWidth
	  val h = window.innerHeight
	  val phase = when {
		!ExperimentState.begun                                              -> Instructions
		ExperimentState.complete                                            -> Complete
		ExperimentState.onBreak                                             -> Break
		Date.now() - ExperimentState.lastInteract >= PARAMS.idleThresholdMS -> Inactive
		w < 1200 || h < 750                                                 -> Resize
		working                                                             -> Loading
		else                                                                -> Trial
	  }
	  return phase
	}
  }

}

abstract class EventDispatcher<T>(type: String? = null) {
  val type: String = type ?: this::class.simpleName!!
  protected val beforeDispatchOps = mutableListOf<(T)->Unit>()
  private fun runBeforeDispatchOps(t: T) {
	beforeDispatchOps.forEach { it(t) }
  }

  fun beforeDispatch(op: (T)->Unit) {
	beforeDispatchOps += op
  }

  open fun dispatchToAllHTML(t: T) {
	runBeforeDispatchOps(t)
	val e = CustomEvent(type, object: CustomEventInit {
	  override var detail: Any? = t
	})
	document.allHTMLElementsRecursive().forEach { it.dispatchEvent(e) }
  }
}

abstract class ChangeEventDispatcher<T>(type: String? = null): EventDispatcher<T>(type) {
  private var didFirstDispatch = false
  private var lastDispatch: T? = null
  override fun dispatchToAllHTML(t: T) {
	if (!didFirstDispatch || t != lastDispatch) {
	  lastDispatch = t
	  super.dispatchToAllHTML(t)
	}
  }
}

fun <T> EventTarget.listen(d: EventDispatcher<T>, op: (T)->Unit) {
  addEventListener(d.type, {
	@Suppress("UNCHECKED_CAST") op((it as CustomEvent).detail as T)
  })
}

object PhaseChange: ChangeEventDispatcher<Pair<ExperimentPhase, ExperimentPhase>>() {
  init {
	beforeDispatch { println("Phase Change: $it") }
  }

  fun afterEndOfNext(phase: ExperimentPhase, listener: (ExperimentPhase)->Unit) {
	var op: ((Pair<ExperimentPhase, ExperimentPhase>)->Unit)? = null
	op = { (old: ExperimentPhase, new: ExperimentPhase) ->
	  if (old == phase && new != phase) {
		listener(new)
		beforeDispatchOps.remove(op!!)
	  }
	}
	beforeDispatchOps.add(op)
  }
}

val currentLeftProp: ReadOnlyBindableProperty<Int> = BindableProperty(currentLeft()).apply {
  window.addEventListener("resize", {
	value = currentLeft()
  })
}

//object MyResizeLeft: ChangeEventDispatcher<Int>() {
//
//}

fun AwesomeElement<*>.onlyShowIn(phase: ExperimentPhase) = element.onlyShowIn(phase)
fun HTMLElement.onlyShowIn(phase: ExperimentPhase) {
  hidden = ExperimentPhase.determine() != phase
  listen(PhaseChange) {
	hidden = it.second != phase
  }
}

fun currentLeft() = (window.innerWidth/2) - HALF_WIDTH
//fun HTMLElement.onMyResizeLeft(onLeft: (Int)->Unit) {
//  onLeft(currentLeft())
//  listen(MyResizeLeft) {
//	onLeft(it)
//  }
//}

interface Drawing {
  val imString: String
  val log: MutableList<Pair<Long, String>>
  fun cleanup()
}

class DrawingData(
  indexedIm: IndexedValue<String>
): Drawing {
  companion object {
	val loadingIm = document.body!!.img {
	  todo("loadingIm is not ideal either")
	  hidden = true
	}
  }

  override val imString = indexedIm.value
  val idx = indexedIm.index
  override val log = mutableListOf<Pair<Long, String>>()
  var loadedImage = false
  var loadedIms = 0
  var finishedProcessesingResp = false
  var trial: DrawingTrial? = null

  init {
	get(
	  DATA_FOLDER + "segment_data2" + "${imString}.json"
	) { resp ->
	  val segs = Json.decodeFromString<Map<String, List<List<Boolean>>>>(resp).entries.let {
		if (PARAMS.randomSegmentOrder) it.shuffled() else it
	  }.mapIndexed { index, entry ->
		val ims = (1..5).map {
		  (document.createElement("img") as HTMLImageElement).also {
			loadDiv.appendChild(it)
			it.hidden = true
			it.setOnLoad {
			  loadedIms++
			  runOpIfReady()
			}
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

		Segment(
		  id = segID, pixels = entry.value, highlightIm = highlightIm, selectIm = selectIm, labelledIm = labelledIm,
		  selectLabeledIm = selectLabeledIm, hiLabeledIm = hiLabeledIm, cycleIndex = index
		)
	  }.sortedBy { it.cycleIndex }

	  trial = DrawingTrial(segs, Loop(segs).iterator(), this)
	  runOpIfReady()
	}
	loadingIm.setOnLoad {
	  loadedImage = true
	  runOpIfReady()
	}
	loadingIm.setAttribute("src", "data/all/${imString}_All.png")

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
	override fun toString() = "Segment $id of ${this@DrawingData}"
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
	val labelledCanvas = canvas()

	operator fun contains(pi: PixelIndex): Boolean {
	  return pixels[pi.y][pi.x]
	}
  }

  val loadDiv = document.body!!.div {
	todo("loadDiv is not ideal")
	hidden = true
  }

  private var ranOp = false
  private fun ready() = loadedImage && trial != null && this.loadedIms == trial!!.segments.size*5
  private var onReadyOp: (()->Unit)? = null
  fun whenReady(op: ()->Unit) {
	require(onReadyOp == null)
	onReadyOp = op
	if (ready()) {
	  ranOp = true
	  op()
	}
  }

  private fun runOpIfReady() {
	require(!ranOp)
	if (ready()) {
	  ranOp = true
	  onReadyOp?.invoke()
	}
  }

  override fun cleanup() {
	trial!!.div.remove()
	loadDiv.remove()
  }


}

class DrawingTrial(
  val segments: List<Segment>, val segCycle: ListIterator<Segment>, dData: DrawingData
): Drawing by dData {

  fun registerInteraction(logMessage: String) {
	ExperimentState.lastInteract = Date.now()
	println(logMessage)
	log.add(Date.now().toLong() to logMessage)
  }

  fun <E: Event> interaction(logMessage: String, op: (E)->Unit): (E)->Unit = {
	registerInteraction(logMessage)
	op(it)
  }

  fun segmentOf(pixelIndex: PixelIndex): Segment? {
	return segments.firstOrNull { pixelIndex in it }
  }

  val segmentsWithResponse get() = segments.filter { it.response != null }
  val completionFraction get() = "${segmentsWithResponse.size}/${segments.size}"
  val isFinished get() = segments.all { it.response != null }
  val isNotFinished get() = !isFinished
  var selectedSeg = BindableProperty<Segment?>(null)
  var hoveredSeg = BindableProperty<Segment?>(null)

  override fun toString() = "${this::class.simpleName} for $imString"


  val allButtons = mutableListOf<HTMLButtonElement>()

  fun Segment.showAsLabeled() {
	val shouldBeImageData: ImageData = selectLabeledPixels
	div.selectCanvas.context2D.putImageData(shouldBeImageData, 0.0, 0.0)
  }

  fun switchSegment(next: Boolean, unlabelled: Boolean) {
	select(when {
	  isFinished && unlabelled  -> null
	  selectedSeg.value == null -> when {
		next -> segments.first()
		else -> segments.last()
	  }

	  next                      -> segCycle.first { !unlabelled || it.hasNoResponse }
	  else                      -> segCycle.firstBackwards { !unlabelled || it.hasNoResponse }
	})
  }

  fun nextSeg() {
	switchSegment(next = true, unlabelled = true)
  }

  fun select(seg: Segment?) {    //	println("selecting $seg")
	if (selectedSeg.value == seg) return
	selectedSeg.value = seg
	if (seg == null) {
	  log.add(Date.now().toLong() to "unselected segment")
	  div.selectCanvas.hidden = true
	} else {
	  log.add(Date.now().toLong() to "selected $seg")
	  if (seg.hasResponse) {
		seg.showAsLabeled()
		allButtons.forEach { bb ->
		  bb.disabled = seg.response == bb.innerHTML
		}
	  } else {
		val shouldBeImageData = seg.selectPixels
		div.selectCanvas.context2D.putImageData(shouldBeImageData, 0.0, 0.0)
		allButtons.forEach { bb ->
		  bb.disabled = false
		}
	  }
	  div.selectCanvas.hidden = false
	}
  }

  fun Segment?.selectThis() = select(this)

  fun hover(seg: Segment?) {
	if (seg == hoveredSeg.value) return
	hoveredSeg.value = seg
	div.hoverCanvas.hidden = hoveredSeg.value == null
	if (hoveredSeg.value != null) div.hoverCanvas.context2D.putImageData(
	  if (seg!!.hasResponse) seg.hiLabeledPixels else seg.highlightPixels, 0.0, 0.0
	)
  }

  fun Segment?.hoverThis() = hover(this)

}

fun MouseEvent.pixelIndexIn(el: HTMLElement): PixelIndex? {
  val e = this
  val x = e.clientX - el.offsetLeft
  val y = e.clientY - el.offsetTop
  if (x < 0 || y < 0) return null
  return PixelIndex(x = x, y = y)
}

data class PixelIndex(val x: Int, val y: Int)