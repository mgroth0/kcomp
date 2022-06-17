package matt.sempart.client.state

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import matt.kjs.Loop
import matt.kjs.Path
import matt.kjs.PixelIndex
import matt.kjs.allHTMLElementsRecursive
import matt.kjs.bind.binding
import matt.kjs.bindings.and
import matt.kjs.bindings.isNull
import matt.kjs.bindings.not
import matt.kjs.currentTimeMillis
import matt.kjs.every
import matt.kjs.first
import matt.kjs.firstBackwards
import matt.kjs.handlers.setOnLoad
import matt.kjs.html.elements.HTMLElementWrapper
import matt.kjs.html.elements.div
import matt.kjs.html.elements.img
import matt.kjs.html.elements.img.HTMLImageWrapper
import matt.kjs.html.elements.img.getPixels
import matt.kjs.prop.BindableProperty
import matt.kjs.prop.ReadOnlyBindableProperty
import matt.kjs.prop.VarProp
import matt.kjs.prop.bProp
import matt.kjs.prop.iProp
import matt.kjs.req.Failure
import matt.kjs.req.HTTPRequester
import matt.kjs.req.HTTPType.GET
import matt.kjs.req.HTTPType.POST
import matt.kjs.req.SimpleSuccess
import matt.kjs.req.Success
import matt.kjs.req.SuccessText
import matt.klib.oset.BasicObservableSet
import matt.klib.todo
import matt.sempart.ExperimentData
import matt.sempart.Issue
import matt.sempart.LogMessage
import matt.sempart.QueryParams
import matt.sempart.client.const.DATA_FOLDER
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.SEND_DATA_PREFIX
import matt.sempart.client.const.WIDTH
import matt.sempart.client.errorDiv.errorDiv
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingData.Segment
import matt.sempart.client.state.ExperimentPhase.Companion.currentPhase
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.ExperimentState.working
import matt.sempart.client.state.TrialPhase.FINISHED
import matt.sempart.client.state.TrialPhase.UNSELECTED
import matt.sempart.client.trialdiv.div
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Date
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

fun sendData(d: ExperimentData, callback: ()->Unit = {}) {
  val it = HTTPRequester(
	POST,
	Path(SEND_DATA_PREFIX + Participant.pid),
  ) {
	when (statusCode) {
	  201  -> SimpleSuccess
	  else -> Failure(statusCode, statusText)
	}
  }.send(d)
  when (it) {
	is Success -> callback()
	is Failure -> {
	  ExperimentState.error = it
	}
  }
}

object UI {
  val enabledProp = BindableProperty(true)
  var enabled by enabledProp
  val disabledProp = enabledProp.not()
  val disabled by disabledProp
}

object Participant {
  val pid = URLSearchParams(window.location.href.substringAfter("?")).get(QueryParams.PROLIFIC_PID)
}


object ExperimentState {
  fun interacted() {
	lastInteract = Date.now()
  }

  var error by Delegates.observable<Failure?>(null) { _, _, n ->

	errorDiv.element.innerHTML = n.toString()

	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())

  }

  private var lastInteract = Date.now()
	.apply {    /*only way to check for lastInteract I think*/    /*I could also check every time lastInteract changes, but that could be even more expensive because of mouse moves?*/
	  every(PARAMS.idleCheckPeriodMS.milliseconds) {
		PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
	  }


	  PhaseChange.beforeDispatch {
		if (it.second == Inactive) {
		  sendData(Issue(currentTimeMillis(), "participant went idle and experiment was cancelled"))
		}
	  }


	}

  fun idle() = Date.now() - ExperimentState.lastInteract >= PARAMS.idleThresholdMS
  var finishedScaling by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
  }
  var finishedVid by Delegates.observable(false) { _, _, _ ->
	PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())
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
  Scaling, InstructionsVid, Instructions, Trial, Break, Complete, Inactive, Resize, Loading, Err;

  companion object {
	val currentPhase: ReadOnlyBindableProperty<ExperimentPhase> = BindableProperty(determine()).apply {
	  PhaseChange.beforeDispatch {
		if (it.second != value) value = it.second
	  }
	}

	fun determine(): ExperimentPhase {
	  val w = window.innerWidth
	  val h = window.innerHeight
	  return when {
		ExperimentState.error != null    -> Err
		!ExperimentState.finishedScaling -> Scaling
		!ExperimentState.finishedVid     -> InstructionsVid
		!ExperimentState.begun           -> Instructions
		ExperimentState.complete         -> Complete
		ExperimentState.onBreak          -> Break
		ExperimentState.idle()           -> Inactive
		w < 1200 || h < 750              -> Resize
		working                          -> Loading
		else                             -> Trial
	  }
	}

	init {
	  window.addEventListener("resize", {
		PhaseChange.dispatchToAllHTML(determine().let { it to it })
	  })
	}

  }

}

enum class TrialPhase {
  UNSELECTED, SELECTED_UNLABELLED, SELECTED_LABELLED, FINISHED
}

abstract class EventDispatcher<T>(type: String? = null) {
  val type: String = type ?: this::class.simpleName!!
  protected val beforeDispatchOps = mutableListOf<(T)->Unit>()
  private fun runBeforeDispatchOps(t: T) {
	println(
	  "beforeDispatchOps.size=${beforeDispatchOps.size}"
	)    /*i might modify the list of ops during this iteration so... WOW THIS FIXED IT*/
	beforeDispatchOps.toList().forEach { it(t) }
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

fun <T> HTMLElementWrapper<*>.listen(d: EventDispatcher<T>, op: (T)->Unit) {
  element.addEventListener(d.type, {
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
	  println("maybe running afterEndOfNext(${phase})")
	  if (old == phase && new != phase) {
		println("yes running")
		beforeDispatchOps.remove(op!!)
		listener(new)
		println("ran")
	  }
	}
	beforeDispatchOps.add(op)
  }
}


class TrialLog(
  private val log: MutableList<LogMessage> = mutableListOf()
) {
  fun get() = log.toList()
  operator fun plusAssign(s: String) {
	log.add(LogMessage(Date.now().toLong(), s))
  }
}


interface Drawing {
  val baseImageName: String
  val log: TrialLog
  fun cleanup()
}

object Something {
  init {
	img {

	}
  }
}

class DrawingData(
  indexedIm: IndexedValue<String>
): Drawing {
  companion object {
	val loadingIm = img {
	  todo("loadingIm is not ideal either")
	  hidden = true
	}.also { document.body!!.append(it.element) }

	//	  (document.body!! as HTMLBodyElement).append
	//	  .
  }

  override val baseImageName = indexedIm.value
  val idx = indexedIm.index
  override val log = TrialLog()
  val loadedImage = bProp(false)
  var loadedIms = iProp(0)
  var finishedProcessesingResp = false
  var trial = VarProp<DrawingTrial?>(null)

  val loadDiv = /*document.body!!.*/div {
	todo("loadDiv is not ideal")
	hidden = true
  }

  val ready = loadedImage.binding(trial, loadedIms) {
	it && trial.value != null && loadedIms.value == trial.value!!.segments.size*5
  }.apply {
	onChange {
	  println("ready=$it (trial=${trial.value},loadedIms=${loadedIms.value},loadedImage=${loadedImage})")
	}
  }

  init {
	HTTPRequester(
	  GET,
	  DATA_FOLDER + "segment_data2" + "${baseImageName}.json",
	  responses = {
		println("statusCode=$statusCode, readyState=${readyState}")
		when (statusCode) {
		  200  -> SuccessText(responseText)
		  else -> Failure(statusCode, statusText)
		}
	  }
	).sendAsync { resp ->
	  println("resp=${resp}")
	  when (resp) {
		is Failure     -> {
		  ExperimentState.error = resp
		}

		is SuccessText -> {
		  //		  println("resp.text:${resp.text}")
		  val segs = Json.decodeFromString<Map<String, List<List<Boolean>>>>(resp.text!!).entries.let {
			if (PARAMS.randomSegmentOrder) it.shuffled() else it
		  }.mapIndexed { index, entry ->
			val ims = (1..5).map {
			  loadDiv.img {
				hidden = true
				setOnLoad {
				  loadedIms.value++
				}
			  }
			  //			  (document.createElement("img") as HTMLImageElement).also {
			  //				loadDiv.appendChild(it)
			  //				it.hidden = true
			  //				it.setOnLoad {
			  //
			  //				}
			  //			  }
			}
			val (highlightIm, selectIm, labelledIm, selectLabeledIm, hiLabeledIm) = ims

			val segID = entry.key


			val imFileName = Path("${baseImageName}_L${segID}.png")

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

		  println("about to set trial.value")

		  trial.value = DrawingTrial(segs, Loop(segs).iterator(), this)
		  println("set trial.value")
		}
	  }
	}
	loadingIm.setOnLoad {
	  loadedImage.value = true
	}
	loadingIm.src = "data/all/${baseImageName}_All.png"
  }


  inner class Segment(
	val id: String,
	val pixels: List<List<Boolean>>,
	val highlightIm: HTMLImageWrapper,
	val selectIm: HTMLImageWrapper,
	val labelledIm: HTMLImageWrapper,
	val selectLabeledIm: HTMLImageWrapper,
	val hiLabeledIm: HTMLImageWrapper,
	val cycleIndex: Int
  ) {
	val responseProp = BindableProperty<String?>(null)
	var response by responseProp
	val hasResponseProp = responseProp.isNull().not()
	val hasResponse get() = hasResponseProp.value
	val hasNoResponse get() = !hasResponse
	override fun toString() = "Segment $id of ${this@DrawingData}"
	val highlightPixels by lazy {
	  highlightIm.getPixels()
	}

	//	val selectCanvas = canvas()
	//	val selectLabeledCanvas = canvas()
	val hiLabeledPixels by lazy {
	  hiLabeledIm.getPixels()
	}    //	val labelledCanvas = canvas()

	operator fun contains(pi: PixelIndex): Boolean {
	  if (pi.x < 0 || pi.y < 0 || pi.x >= WIDTH || pi.y >= HEIGHT) return false
	  return pixels[pi.y][pi.x]
	}


  }



  fun whenReady(op: ()->Unit) {
	if (ready.value) op()
	else {
	  ready.onChangeUntil({ it }) {
		if (it) op()
	  }
	}
  }


  override fun cleanup() {
	trial.value!!.div.element.remove()
	loadDiv.remove()
  }


}

class DrawingTrial(
  val segments: List<Segment>, val segCycle: ListIterator<Segment>, dData: DrawingData
): Drawing by dData {

  val phaseProp = VarProp(UNSELECTED)
  var phase by phaseProp

  fun registerInteraction(logMessage: String) {
	ExperimentState.interacted()
	println(logMessage)
	log += logMessage
  }

  fun <E: Event> interaction(
	logMessage: String, disableUI: Boolean = false, op: (E)->Unit
  ): (E)->Unit = {
	registerInteraction(logMessage)
	if (disableUI) UI.enabled = false
	op(it)
	if (disableUI) UI.enabled = true
  }

  fun segmentOf(pixelIndex: PixelIndex): Segment? {
	return segments.firstOrNull { pixelIndex in it }
  }

  val segmentsWithResponse get() = segments.filter { it.hasResponse }
  val completionFraction get() = "${segmentsWithResponse.size}/${segments.size}"

  //  fun redraw() {
  //	segments.forEach { it.redraw() }
  //  }

  //  fun Segment.redraw() {
  //	labelledCanvas.showing = hasResponse
  //	selectCanvas.showing = this in selectedSegments
  //	selectLabeledCanvas.showing = hasResponse && this in selectedSegments
  //  }

  val finishedProp = segments.map { it.hasResponseProp }.reduce { r1, r2 -> r1.and(r2) }.apply {
	onChange {
	  if (it) phase = FINISHED
	}
  }
  val isFinished get() = finishedProp.value
  val isNotFinished get() = !isFinished
  var selectedSegments = BasicObservableSet<Segment>()

  /*val selectedSegResponse = selectedSeg.chainBinding {
	it?.responseProp
  }*/
  var hoveredSeg = BindableProperty<Segment?>(null)

  override fun toString() = "${this::class.simpleName} for $baseImageName"


  fun switchSegment(next: Boolean, unlabelled: Boolean) {
	val wasEmpty = segments.isEmpty()
	if (PARAMS.allowMultiSelection) selectedSegments.clear()
	select(when {
	  isFinished && unlabelled -> {
		if (!PARAMS.allowMultiSelection) selectedSegments.clear()
		return
	  }

	  wasEmpty                 -> when {
		next -> segments.first()
		else -> segments.last()
	  }

	  next                     -> segCycle.first { !unlabelled || it.hasNoResponse }
	  else                     -> segCycle.firstBackwards { !unlabelled || it.hasNoResponse }
	})
  }


  fun select(seg: Segment) {
	registerInteraction("selected $seg")
	selectedSegments.add(seg)
  }

  fun hover(seg: Segment?) {
	if (seg == hoveredSeg.value) return
	hoveredSeg.value = seg
  }

}

