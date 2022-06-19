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
import matt.kjs.html.elements.HTMLElementWrapper
import matt.kjs.html.elements.img.HTMLImageWrapper
import matt.kjs.html.elements.img.Img
import matt.kjs.html.elements.img.getPixels
import matt.kjs.html.elements.img.preload
import matt.kjs.prop.BindableProperty
import matt.kjs.prop.ReadOnlyBindableProperty
import matt.kjs.prop.VarProp
import matt.kjs.prop.iProp
import matt.kjs.req.Failure
import matt.kjs.req.HTTPRequester
import matt.kjs.req.HTTPType.GET
import matt.kjs.req.HTTPType.POST
import matt.kjs.req.SimpleSuccess
import matt.kjs.req.Success
import matt.kjs.req.SuccessText
import matt.klib.oset.BasicObservableSet
import matt.sempart.ExperimentData
import matt.sempart.Issue
import matt.sempart.LogMessage
import matt.sempart.QueryParams
import matt.sempart.SegmentResponse
import matt.sempart.TrialData
import matt.sempart.client.const.DATA_FOLDER
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.SEND_DATA_PREFIX
import matt.sempart.client.const.WIDTH
import matt.sempart.client.errorDiv.errorDiv
import matt.sempart.client.params.PARAMS
import matt.sempart.client.scaleDiv.neededHeight
import matt.sempart.client.scaleDiv.neededWidth
import matt.sempart.client.scaleDiv.scaleProp
import matt.sempart.client.state.DrawingData.ImName.HiLabelledIm
import matt.sempart.client.state.DrawingData.ImName.SegmentHighlighted
import matt.sempart.client.state.DrawingData.ImName.SegmentLabelled
import matt.sempart.client.state.DrawingData.ImName.SegmentSelected
import matt.sempart.client.state.DrawingData.ImName.SegmentSelectedLabelled
import matt.sempart.client.state.DrawingData.Segment
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.ExperimentState.working
import matt.sempart.client.state.Participant.pid
import matt.sempart.client.state.TrialPhase.FINISHED
import matt.sempart.client.state.TrialPhase.UNSELECTED
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Date
import kotlin.time.Duration.Companion.milliseconds

fun sendData(d: ExperimentData, callback: ()->Unit = {}) {
  HTTPRequester(
	POST,
	Path(SEND_DATA_PREFIX + Participant.pid),
  ) {
	when (statusCode) {
	  201  -> SimpleSuccess
	  else -> Failure(statusCode, statusText)
	}
  }.sendAsync(d) {
	when (it) {
	  is Success -> callback()
	  is Failure -> {
		ExperimentState.error.value = it
	  }
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
  var pid = URLSearchParams(window.location.href.substringAfter("?")).get(QueryParams.PROLIFIC_PID)!!
}


object ExperimentState {
  fun interacted() {
	lastInteract = Date.now()
  }

  var error = VarProp<Failure?>(null).apply {
	onChange {
	  errorDiv.element.innerHTML = it.toString()
	  ExperimentPhase.determineAndEmit()
	}
  }

  private var lastInteract = Date.now()
	.apply {    /*only way to check for lastInteract I think*/    /*I could also check every time lastInteract changes, but that could be even more expensive because of mouse moves?*/
	  every(PARAMS.idleCheckPeriodMS.milliseconds) {
		ExperimentPhase.determineAndEmit()

	  }


	  PhaseChange.beforeDispatch {
		if (it.second == Inactive) {
		  sendData(Issue(pid, currentTimeMillis(), "participant went idle and experiment was cancelled"))
		}
	  }


	}

  fun idle() = Date.now() - ExperimentState.lastInteract >= PARAMS.idleThresholdMS
  var finishedScaling = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var nameIsGood = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var finishedVid = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var begun = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var onBreak = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var complete = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
  var working = VarProp(false).apply {
	onChange { ExperimentPhase.determineAndEmit() }
  }
}

enum class ExperimentPhase {
  Name, Scaling, InstructionsVid, Instructions, Trial, Break, Complete, Inactive, Resize, Loading, Err;

  companion object {
	private val currentPhase: ReadOnlyBindableProperty<ExperimentPhase> = BindableProperty(determine()).apply {
	  PhaseChange.beforeDispatch {
		if (it.second != value) value = it.second
	  }

	}

	fun determineAndEmit() = PhaseChange.dispatchToAllHTML(currentPhase.value to ExperimentPhase.determine())

	fun determine(): ExperimentPhase {
	  lock?.let { return it }
	  val w = window.innerWidth
	  val h = window.innerHeight
	  return when {
		ExperimentState.error.value != null     -> Err
		w < neededWidth() || h < neededHeight() -> Resize
		!ExperimentState.finishedScaling.value  -> Scaling
		!ExperimentState.nameIsGood.value       -> Name
		!ExperimentState.finishedVid.value      -> InstructionsVid
		!ExperimentState.begun.value            -> Instructions
		ExperimentState.complete.value          -> Complete
		ExperimentState.onBreak.value           -> Break
		ExperimentState.idle()                  -> Inactive
		working.value                           -> Loading
		else                                    -> Trial
	  }
	}

	private var lock: ExperimentPhase? = null
	fun lockAt(phase: ExperimentPhase?) {
	  lock = phase
	  determineAndEmit()
	}

	init {
	  window.addEventListener("resize", {
		PhaseChange.dispatchToAllHTML(determine().let { it to it })
	  })
	  scaleProp.onChange {
		ExperimentPhase.determineAndEmit()
	  }
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
	  if (old == phase && new != phase) {
		beforeDispatchOps.remove(op!!)
		listener(new)
	  }
	}
	beforeDispatchOps.add(op)
  }

  fun atStartOf(phase: ExperimentPhase, listener: (ExperimentPhase)->Unit) {
	var op: ((Pair<ExperimentPhase, ExperimentPhase>)->Unit)? = null
	op = { (old: ExperimentPhase, new: ExperimentPhase) ->
	  if (old != phase && new == phase) {
		beforeDispatchOps.remove(op!!)
		listener(new)
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
  val training: Boolean
  val baseIm: Img
}


class DrawingData(
  indexedIm: IndexedValue<String>,
  override val training: Boolean
): Drawing {

  override fun toString(): String {
	return "${DrawingData::class.simpleName} for $baseImageName"
  }

  override val baseImageName = indexedIm.value
  val idx = indexedIm.index
  override val log = TrialLog()
  private var loadedIms = iProp(0)
  var trial = VarProp<DrawingTrial?>(null)
  val ready = loadedIms.binding(trial) {
	trial.value != null && loadedIms.value == trial.value!!.segments.size*5 + 1
  }

  enum class ImName(val s: String) {
	SegmentHighlighted("segment_highlighted"),
	SegmentSelected("segment_selected"),
	SegmentLabelled("segment_labelled"),
	SegmentSelectedLabelled("segment_selected_labeled"),
	HiLabelledIm("segment_hi_labeled")
  }

  init {
	HTTPRequester(
	  GET,
	  DATA_FOLDER + "segment_data2" + "${baseImageName}.json",
	  responses = {
		when (statusCode) {
		  200  -> SuccessText(responseText)
		  else -> Failure(statusCode, statusText)
		}
	  }
	).sendAsync { resp ->
	  when (resp) {
		is Failure     -> {
		  ExperimentState.error.value = resp
		}

		is SuccessText -> {
		  val segs = Json.decodeFromString<Map<String, List<List<Boolean>>>>(resp.text!!).entries.let {
			if (PARAMS.randomSegmentOrder) it.shuffled() else it
		  }.mapIndexed { index, entry ->
			val segID = entry.key
			val imFileName = Path("${baseImageName}_L${segID}.png")
			val ims = ImName.values().associateWith {
			  preload(DATA_FOLDER + it.s + imFileName) {
				loadedIms.value++
			  }
			}
			Segment(
			  id = segID,
			  pixels = entry.value,
			  highlightIm = ims[SegmentHighlighted]!!,
			  selectIm = ims[SegmentSelected]!!,
			  labelledIm = ims[SegmentLabelled]!!,
			  selectLabeledIm = ims[SegmentSelectedLabelled]!!,
			  hiLabeledIm = ims[HiLabelledIm]!!,
			  cycleIndex = index
			)
		  }.sortedBy { it.cycleIndex }
		  trial.value = DrawingTrial(segs, Loop(segs).iterator(), this, idx)
		}
	  }
	}
  }

  override val baseIm = preload(Path("data") + "all" + (baseImageName + "_All.png")) {
	loadedIms.value++
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

	val hiLabeledPixels by lazy {
	  hiLabeledIm.getPixels()
	}

	operator fun contains(pi: PixelIndex): Boolean {
	  if (pi.x < 0 || pi.y < 0 || pi.x >= WIDTH || pi.y >= HEIGHT) return false
	  return pixels[pi.y][pi.x]
	}
  }
}

class DrawingTrial(
  val segments: List<Segment>,
  private val segCycle: ListIterator<Segment>,
  dData: DrawingData,
  val idx: Int
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

  val finishedProp = segments.map { it.hasResponseProp }.reduce { r1, r2 -> r1.and(r2) }.apply {
	onChange {
	  if (it) phase = FINISHED
	}
  }
  val isFinished get() = finishedProp.value
  val isNotFinished get() = !isFinished
  var selectedSegments = BasicObservableSet<Segment>()
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

  fun data() = TrialData(
	pid = pid,
	image = baseImageName,
	index = idx,
	responses = segments.map { SegmentResponse(it.id, it.response!!) },
	trialLog = log.get()
  )

}

