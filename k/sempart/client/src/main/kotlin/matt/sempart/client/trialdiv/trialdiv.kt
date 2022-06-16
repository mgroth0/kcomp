package matt.sempart.client.trialdiv

import kotlinx.html.ButtonType
import matt.kjs.WeakMap
import matt.kjs.bind.binding
import matt.kjs.bindings.eq
import matt.kjs.bindings.isNull
import matt.kjs.bindings.not
import matt.kjs.bindings.or
import matt.kjs.bindings.orDebug
import matt.kjs.css.Display.inlineBlock
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
import matt.kjs.props.innerHTMLProperty
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
import matt.sempart.client.state.TrialPhase.FINISHED
import matt.sempart.client.state.TrialPhase.SELECTED_LABELLED
import matt.sempart.client.state.TrialPhase.SELECTED_UNLABELLED
import matt.sempart.client.state.TrialPhase.UNSELECTED
import matt.sempart.client.state.UI
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.sty.box
import matt.sempart.client.sty.boxButton
import matt.sempart.client.sty.centerInParent
import matt.sempart.client.ui.boxButton
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Date

interface TrialDiv: HTMLElementWrapper<HTMLDivElement> {
  val nextImageButton: HTMLButtonElement
  val mainCanvas: HTMLCanvasElement
  val hoverCanvas: HTMLCanvasElement
  val selectCanvas: HTMLCanvasElement
  val helpText: HTMLParagraphElement
}

private val trialsDivs = WeakMap<DrawingTrial, TrialDiv>().withStoringDefault { it.trialDiv() }
val DrawingTrial.div: TrialDiv get() = trialsDivs[this]

private fun DrawingTrial.trialDiv(): TrialDiv = object: AwesomeElement<HTMLDivElement>(), TrialDiv {

  fun eventToSeg(e: MouseEvent) = e.pixelIndexIn(mainCanvas)?.let { segmentOf(it) }

  override val element = div {
	//	classList.add(mainDivClass)
	//	sty.transform = Transform().apply {
	//	  map["scale"] = scaleDiv.sty.transform.map["scale"]!!
	//	}
	onlyShowIn(Trial)
	//	sty.marginLeftProperty().bind(currentLeftProp)
	sty.centerInParent()
	//	sty.transform = sty.transform.apply {
	//	  map["translate"] = listOf(-HALF_WIDTH)
	//	}
	sty {
	  height = HEIGHT.px // + 200.px
	  width = WIDTH.px + 300.px
	}
  }
  val stackDiv = element.div {
	//	sty.
	sty {
//	  verticallyCenterInParent()
	  width = WIDTH.px
	  height = HEIGHT.px
	  display = inlineBlock
	}
  }

  private fun HTMLCanvasElement.canvasConfig(idx: Int) {
	width = WIDTH
	height = HEIGHT
	sty {
	  //	  centerOnWindow()
	  position = absolute
	  top = 0.px
	  left = 0.px
	  zIndex = idx
	  //	  top = 0.px
	  //	  leftProperty().bind(currentLeftProp)
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
	setOnMouseMove {
	  ExperimentState.lastInteract = Date.now()
	  hover(eventToSeg(it))
	}
	onclick = interaction("click") {
	  println("click.clientX = ${it.clientX}")
	  println("click.clientY = ${it.clientY}")
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
			//			sty.centerOnWindow()
			position = absolute
			top = 0.px
			left = 0.px
			zIndex = zIdx
			//			leftProperty().bind(currentLeftProp)
		  }
		  context2D.drawImage(theSeg.labelledIm, 0.0, 0.0)
		}, stackDiv.children[zIdx]
	  )
	}
  }

  val controlsDiv: HTMLDivElement = element.div {
	sty {
//	  verticallyCenterInParent()
//	  left = WIDTH.px
	  //	  position = relative
	  //	  left = WIDTH.px
	  width = 300.px
	  height = HEIGHT.px
	  display = inlineBlock
	  //	  sty.centerOnWindow()
	}
	sty {
	  //	  leftProperty().bind(currentLeftProp.binding { it + WIDTH })
	  marginBottom = MED_SPACE
	}
  }

  val labelsDiv = controlsDiv.div {
	hiddenProperty().bind(selectedSeg.isNull())
	sty {
	  box()
	}
	(LABELS.shuffled() + "Something else" + "I don't know").forEach { l ->
	  boxButton {
		disabledProperty().bind(UI.disabledProp orDebug selectedSegResponse.eq(l))
		innerHTML = l
		sty.fontStyle = italic
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

  val previousSegmentButton = regularNextSegButtonBox.boxButton {
	disabledProperty().bind(UI.disabledProp)
	innerHTML = "Previous Segment"
	onclick = interaction("previousSegmentButton clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = false)
	}
  }

  val nextSegmentButton = regularNextSegButtonBox.boxButton {
	disabledProperty().bind(UI.disabledProp)
	innerHTML = "Next Segment"
	onclick = interaction("nextSegmentButton.onclick", disableUI = true) {
	  switchSegment(next = true, unlabelled = false)
	}
  }

  private val unlabelledString = if (PARAMS.removeNpButtonsKeepUnlabelledNpButtons) "" else " Unlabeled"

  val previousUnlabeledSegmentButton = buttonsDiv.boxButton {
	disabledProperty().bind(UI.disabledProp or finishedProp)
	innerHTML = "Previous$unlabelledString Segment"
	onclick = interaction("previous unlabeled segment button clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = true)
	}
  }


  val nextUnlabeledSegmentButton = buttonsDiv.boxButton {
	disabledProperty().bind(UI.disabledProp.or(finishedProp))
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

  override val helpText = controlsDiv.p {
	innerHTMLProperty().bind(phaseProp.binding {
	  "This first drawing is for training purposes only and the data will not be used.<br><br>" + when (it) {
		UNSELECTED          -> "You may select a segment by clicking it. You may also click the \"${nextUnlabeledSegmentButton.innerHTML}\" button or the \"${previousUnlabeledSegmentButton.innerHTML}\" button to cycle through unselected segments automatically."
		SELECTED_UNLABELLED -> "Now that a segment is selected, you may choose a label. Please choose the label by clicking the label button that you think best fits this segment. If you change your mind after selecting a label you can reselect the segment and change your response. After choosing a label for a segment for the first time, the next unlabelled segment will automatically be selected for you."
		SELECTED_LABELLED   -> "This segment is already labelled. You can still change which label you are assigning to this segment by clicking one of the label buttons."
		FINISHED            -> "Great job. You have chosen a label for every segment in this drawing. You can still go back and change your responses before continuing. Once ready, click the \"${nextImageButton.innerHTML}\" button. After moving onto the next drawing, you will not be able to come back and change your responses on the current drawing."
	  }
	})
  }

}