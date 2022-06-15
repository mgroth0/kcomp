package matt.sempart.client.trialdiv

import kotlinx.css.Display.inlineBlock
import kotlinx.html.ButtonType
import matt.kjs.WeakMap
import matt.kjs.bind.binding
import matt.kjs.bindings.eq
import matt.kjs.bindings.isNull
import matt.kjs.bindings.not
import matt.kjs.bindings.or
import matt.kjs.bindings.orDebug
import matt.kjs.css.FontStyle.italic
import matt.kjs.css.FontWeight.bold
import matt.kjs.css.Position.absolute
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.AwesomeElement
import matt.kjs.elements.HTMLElementWrapper
import matt.kjs.img.context2D
import matt.kjs.pixelIndexIn
import matt.kjs.props.disabledProperty
import matt.kjs.props.hiddenProperty
import matt.kjs.props.leftProperty
import matt.kjs.props.marginLeftProperty
import matt.kjs.setOnMouseMove
import matt.klib.dmap.withStoringDefault
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.LABELS
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.state.DrawingData.Companion.loadingIm
import matt.sempart.client.state.DrawingData.Segment
import matt.sempart.client.state.DrawingTrial
import matt.sempart.client.state.ExperimentPhase.Trial
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.UI
import matt.sempart.client.state.currentLeftProp
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.sty.box
import matt.sempart.client.sty.boxButton
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Date

interface TrialDiv: HTMLElementWrapper<HTMLDivElement> {
  val nextImageButton: HTMLButtonElement
  val mainCanvas: HTMLCanvasElement
  val hoverCanvas: HTMLCanvasElement
  val selectCanvas: HTMLCanvasElement
}

private val trialsDivs = WeakMap<DrawingTrial, TrialDiv>().withStoringDefault { it.trialDiv() }
val DrawingTrial.div: TrialDiv get() = trialsDivs[this]

private fun DrawingTrial.trialDiv(): TrialDiv = object: AwesomeElement<HTMLDivElement>(), TrialDiv {

  fun eventToSeg(e: MouseEvent) = e.pixelIndexIn(mainCanvas)?.let { segmentOf(it) }

  override val element = div {
	onlyShowIn(Trial)
	sty.marginLeftProperty().bind(currentLeftProp)
  }
  val stackDiv = element.div {
	sty.display = inlineBlock
  }

  private fun HTMLCanvasElement.canvasConfig(idx: Int) {
	width = WIDTH
	height = HEIGHT
	sty {
	  position = absolute
	  zIndex = idx
	  top = 0.px
	  leftProperty().bind(currentLeftProp)
	}
	if (idx > 1) {
	  sty.zIndex = idx + segments.size
	}
  }

  override val mainCanvas = stackDiv.canvas {
	canvasConfig(0)
	context2D.drawImage(loadingIm, 0.0, 0.0)
  }
  override val hoverCanvas = stackDiv.canvas {
	hidden = true
	canvasConfig(1)
  }
  override val selectCanvas = stackDiv.canvas {
	hidden = true
	canvasConfig(2)
  }
  val eventCanvasIDK = stackDiv.canvas {
	canvasConfig(3)
	onclick = interaction("click") {
	  select(eventToSeg(it))
	}
	setOnMouseMove {
	  ExperimentState.lastInteract = Date.now()
	  hover(eventToSeg(it))
	}
	onclick = interaction("click") {
	  select(eventToSeg(it))
	}
  }

  init {
	segments.forEach { theSeg: Segment ->
	  val zIdx = theSeg.cycleIndex + 1
	  stackDiv.insertBefore(
		theSeg.labelledCanvas.apply {
		  width = WIDTH
		  height = HEIGHT
		  hidden = true
		  sty {
			position = absolute
			top = 0.px
			zIndex = zIdx
			leftProperty().bind(currentLeftProp)
		  }
		  context2D.drawImage(theSeg.labelledIm, 0.0, 0.0)
		}, stackDiv.children[zIdx]
	  )
	}
  }

  val controlsDiv: HTMLDivElement = element.div {
	sty {
	  display = inlineBlock
	  position = absolute
	}
	sty {
	  leftProperty().bind(currentLeftProp.binding { it + WIDTH })
	  marginBottom = MED_SPACE
	}
  }

  val labelsDiv = controlsDiv.div {
	hiddenProperty().bind(selectedSeg.isNull())
	sty {
	  box()
	}
	(LABELS.shuffled() + "Something else" + "I don't know").forEach { l ->
	  button {
		disabledProperty().bind(UI.disabledProp orDebug selectedSegResponse.eq(l))
		innerHTML = l
		sty {
		  boxButton()
		  fontStyle = italic
		}
		onclick = interaction("selected label: $l") {
		  selectedSeg.value!!.labelledCanvas.hidden = false
		  val hadNoResponse = selectedSeg.value!!.hasNoResponse
		  selectedSeg.value!!.response = l
		  completionP.innerHTML = "$completionFraction segments labelled"
		  if (hadNoResponse) {
			selectedSeg.value!!.showAsLabeled()
			nextSeg()
		  }
		}
	  }
	}
  }

  val buttonsDiv = controlsDiv.div {
	sty.box()
  }

  val regularNextSegButtonBox = buttonsDiv.div {
	hidden = PARAMS.removeNpButtonsKeepUnlabelledNpButtons
  }

  val previousSegmentButton = regularNextSegButtonBox.button {
	disabledProperty().bind(UI.disabledProp)
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Previous Segment"
	onclick = interaction("previousSegmentButton clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = false)
	}
  }

  val nextSegmentButton = regularNextSegButtonBox.button {
	disabledProperty().bind(UI.disabledProp)
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Next Segment"
	onclick = interaction("nextSegmentButton.onclick", disableUI = true) {
	  switchSegment(next = true, unlabelled = false)
	}
  }

  private val unlabelledString = if (PARAMS.removeNpButtonsKeepUnlabelledNpButtons) " Unlabeled" else ""

  val previousUnlabeledSegmentButton = buttonsDiv.button {
	disabledProperty().bind(UI.disabledProp.or(finishedProp))
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Previous$unlabelledString Segment"
	onclick = interaction("previous unlabeled segment button clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = true)
	}
  }


  val nextUnlabeledSegmentButton = buttonsDiv.button {
	disabledProperty().bind(UI.disabledProp.or(finishedProp))
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Next$unlabelledString Segment"
	onclick = interaction("next unlabeled segment button clicked", disableUI = true) {
	  switchSegment(next = true, unlabelled = true)
	}
  }

  val completionP = controlsDiv.p {
	innerHTML = "$completionFraction segments labelled"
  }

  override val nextImageButton = controlsDiv.button {
	disabledProperty().bind(finishedProp.not())
	type = ButtonType.button.realValue
	sty {
	  fontWeight = bold
	  boxButton()
	  innerHTML = "Submit Responses and Show Next Image"
	}
  }

}