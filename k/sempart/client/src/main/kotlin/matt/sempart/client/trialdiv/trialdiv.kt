package matt.sempart.client.trialdiv

import kotlinx.html.ButtonType
import matt.kjs.WeakMap
import matt.kjs.bind.binding
import matt.kjs.bindings.isNull
import matt.kjs.css.Display.InlineBlock
import matt.kjs.css.FontStyle.italic
import matt.kjs.css.FontWeight.bold
import matt.kjs.css.Position.absolute
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.AwesomeElement
import matt.kjs.elements.HTMLElementWrapper
import matt.kjs.img.context2D
import matt.kjs.props.hiddenProperty
import matt.kjs.props.leftProperty
import matt.kjs.props.marginLeftProperty
import matt.kjs.setOnClick
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
import matt.sempart.client.state.currentLeftProp
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.state.pixelIndexIn
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
	sty.display = InlineBlock
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
	  display = InlineBlock
	  position = absolute
	}
	sty.leftProperty().bind(currentLeftProp.binding { it + WIDTH })

  }

  val labelsDiv = controlsDiv.div {
	hiddenProperty().bind(selectedSeg.isNull())
	sty {
	  box()
	  marginBottom = MED_SPACE
	}
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
		  log += "selected label: $l"

		  println("getting labelledCanvas of $selectedSeg")
		  selectedSeg.value!!.labelledCanvas.hidden = false

		  val hadResponse = selectedSeg.value!!.response != null
		  selectedSeg.value!!.response = l
		  completionP.innerHTML = "$completionFraction segments labelled"

		  allButtons.forEach { bb ->
			bb.disabled = bb.innerHTML == l
		  }

		  nextImageButton.disabled = isNotFinished
		  nextUnlabeledSegmentButton.disabled = isFinished
		  previousUnlabeledSegmentButton.disabled = isFinished
		  if (!hadResponse) {
			selectedSeg.value!!.showAsLabeled()
			nextSeg()
		  }
		}
	  })
	  br
	}
  }
//
  //  init {
  //	controlsDiv.br
  //	controlsDiv.br
  //  }

  val buttonsDiv = controlsDiv.div {
	sty.box()
  }

  val regularNextSegButtonBox = buttonsDiv.div {
	hidden = PARAMS.removeNpButtonsKeepUnlabelledNpButtons
  }

  val previousSegmentButton = regularNextSegButtonBox.button {
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

  init {
	regularNextSegButtonBox.br {
	  id = "previousSegmentButtonBR"
	}
  }

  val nextSegmentButton = regularNextSegButtonBox.button {
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

  init {
	regularNextSegButtonBox.br {
	  id = "nextSegmentButtonBR"
	}
  }

  private val unlabelledString = if (PARAMS.removeNpButtonsKeepUnlabelledNpButtons) " Unlabeled" else ""

  val previousUnlabeledSegmentButton = buttonsDiv.button {
	disabled = false
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Previous$unlabelledString Segment"
	setOnClick {
	  console.log("previous unlabeled segment button clicked")
	  ExperimentState.lastInteract = Date.now()
	  disabled = true
	  switchSegment(next = false, unlabelled = true)
	  disabled = false
	}
  }


  init {
	buttonsDiv.br
  }

  val nextUnlabeledSegmentButton = buttonsDiv.button {
	disabled = false
	type = ButtonType.button.realValue
	sty.boxButton()
	innerHTML = "Next$unlabelledString Segment"
	setOnClick {
	  console.log("next unlabeled segment button clicked")
	  ExperimentState.lastInteract = Date.now()
	  disabled = true
	  switchSegment(next = true, unlabelled = true)
	  disabled = false
	}
  }

  init {
	controlsDiv.br
	controlsDiv.br
  }

  val completionP = controlsDiv.p {
	innerHTML = "$completionFraction segments labelled"
  }

  override val nextImageButton = controlsDiv.button {
	disabled = true
	type = ButtonType.button.realValue
	sty {
	  fontWeight = bold
	  boxButton()
	  innerHTML = "Submit Responses and Show Next Image"
	}
  }

}