package matt.sempart.client.trialdiv

import kotlinx.html.ButtonType
import matt.kjs.WeakMap
import matt.kjs.bind.binding
import matt.kjs.bindings.isEmptyProperty
import matt.kjs.bindings.not
import matt.kjs.bindings.or
import matt.kjs.css.FlexDirection.row
import matt.kjs.css.FontStyle.italic
import matt.kjs.css.FontWeight.bold
import matt.kjs.css.Position.absolute
import matt.kjs.css.Transform.Scale
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.handlers.setOnMouseMove
import matt.kjs.html.elements.HTMLElementWrapper
import matt.kjs.html.elements.appendWrapper
import matt.kjs.html.elements.canvas.HTMLCanvasWrapper
import matt.kjs.html.elements.canvas.draw
import matt.kjs.html.elements.canvas.put
import matt.kjs.html.elements.div.HTMLDivWrapper
import matt.kjs.html.elements.img.HTMLImageWrapper
import matt.kjs.node.HoldButton
import matt.kjs.pixelIndexInTarget
import matt.kjs.props.disabledProperty
import matt.kjs.props.hiddenProperty
import matt.kjs.props.innerHTMLProperty
import matt.kjs.showing
import matt.klib.dmap.withStoringDefault
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.LABELS
import matt.sempart.client.const.WIDTH
import matt.sempart.client.params.PARAMS
import matt.sempart.client.scaleDiv.scaleDiv
import matt.sempart.client.scaleDiv.scaleProp
import matt.sempart.client.state.DrawingData.Segment
import matt.sempart.client.state.DrawingTrial
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.ExperimentPhase.Trial
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.TrialPhase.FINISHED
import matt.sempart.client.state.TrialPhase.SELECTED_LABELLED
import matt.sempart.client.state.TrialPhase.SELECTED_UNLABELLED
import matt.sempart.client.state.TrialPhase.UNSELECTED
import matt.sempart.client.state.UI
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.sty.box
import matt.sempart.client.sty.boxButton
import matt.sempart.client.ui.ExperimentScreen
import matt.sempart.client.ui.boxButton
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent

interface TrialDiv: HTMLElementWrapper<HTMLDivElement> {
  val nextImageButton: HoldButton
}


@Suppress("UNUSED_EXPRESSION")
open class ImageAndControlsScreen(
  phase: ExperimentPhase,
  cfg: ImageAndControlsScreen.()->Unit = {}
): ExperimentScreen(
  phase,
  flexDir = row
) {

  companion object {
	val TOTAL_WIDTH = WIDTH * 2
  }

  val stackDiv = div {
	sty {
	  width = WIDTH.px
	  height = HEIGHT.px
	}
  }
  val controlsDiv: HTMLDivWrapper = div {
	sty {
	  marginBottom = MED_SPACE
	  width = WIDTH.px
	}
  }
  val nothing = cfg()
}

private val trialsDivs = WeakMap<DrawingTrial, TrialDiv>().withStoringDefault { it.trialDiv() }
val DrawingTrial.div: TrialDiv get() = trialsDivs[this]

private fun DrawingTrial.trialDiv(): TrialDiv = object: ImageAndControlsScreen(
  Trial,
), TrialDiv {

  init {
	element.sty.resetTransform {
	  scale(scaleProp.value.toDouble())
	}
	scaleProp.onChange {
	  element.sty.resetTransform {
		scale(scaleDiv.sty.transform.funs.filterIsInstance<Scale>().first().args)
	  }
	}
  }

  fun eventToSeg(e: MouseEvent) = segmentOf(e.pixelIndexInTarget())

  private var zIdx = 0

  init {
	stackDiv.withConfig {
	  fun stackCanvas(
		im: HTMLImageWrapper?,
		hide: Boolean = true,
		op: HTMLCanvasWrapper.()->Unit = {}
	  ) {
		canvas {
		  width = WIDTH
		  height = HEIGHT
		  sty {
			position = absolute
			zIndex = zIdx++
		  }
		  im?.let { draw(it) }
		  hidden = hide
		  op()
		}
	  }
	  stackCanvas(baseIm, hide = false)
	  stackCanvas(im = null) {
		hoveredSeg.onChange {
		  showing = it != null
		  if (it != null) put(if (it.hasResponse) it.hiLabeledPixels else it.highlightPixels)
		}
	  }

	  segments.forEach { theSeg: Segment ->
		stackCanvas(theSeg.labelledIm) {
		  hiddenProperty().bind(theSeg.hasResponseProp.not())
		}
		stackCanvas(theSeg.selectIm) {
		  hiddenProperty().bind(selectedSegments.binding { theSeg !in it })
		}
		stackCanvas(theSeg.selectLabeledIm) {
		  hiddenProperty().bind(selectedSegments.binding(theSeg.responseProp) { theSeg !in it || theSeg.hasNoResponse })
		}
	  }
	  stackCanvas(im = null, hide = false) {
		setOnMouseMove {
		  ExperimentState.interacted()
		  hover(eventToSeg(it))
		}
		onclick = interaction("click") {
		  val seg = eventToSeg(it)
		  if (seg == null) selectedSegments.clear()
		  else if (seg !in selectedSegments) {
			if (!PARAMS.allowMultiSelection || !it.shiftKey) selectedSegments.clear()
			select(seg)
		  }
		}
	  }
	}
  }


  val labelsDiv = controlsDiv.div {
	hiddenProperty().bind(selectedSegments.isEmptyProperty())
	sty {
	  box()
	}
	(LABELS.shuffled() + "Something else" + "I don't know").forEach { l ->
	  boxButton {
		disabledProperty().bind(UI.disabledProp or selectedSegments.binding { it.all { it.response == l } })
		+l
		sty.fontStyle = italic
		onclick = interaction("selected label: $l") {
		  val gotFirstResponse = selectedSegments.filter {
			val hadNoResponse = it.hasNoResponse
			it.response = l
			hadNoResponse
		  }
		  completionP.innerText = "$completionFraction segments labelled"
		  if (gotFirstResponse.isNotEmpty()) switchSegment(next = true, unlabelled = true)
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
	innerText = "Previous Segment"
	onclick = interaction("previousSegmentButton clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = false)
	}
  }

  val nextSegmentButton = regularNextSegButtonBox.boxButton {
	disabledProperty().bind(UI.disabledProp)
	innerText = "Next Segment"
	onclick = interaction("nextSegmentButton.onclick", disableUI = true) {
	  switchSegment(next = true, unlabelled = false)
	}
  }

  private val unlabelledString = if (PARAMS.removeNpButtonsKeepUnlabelledNpButtons) "" else " Unlabeled"

  val previousUnlabeledSegmentButton = buttonsDiv.boxButton {
	disabledProperty().bind(UI.disabledProp or finishedProp)
	innerText = "Previous$unlabelledString Segment"
	onclick = interaction("previous unlabeled segment button clicked", disableUI = true) {
	  switchSegment(next = false, unlabelled = true)
	}
  }


  val nextUnlabeledSegmentButton = buttonsDiv.boxButton {
	disabledProperty().bind(UI.disabledProp.or(finishedProp))
	innerText = "Next$unlabelledString Segment"
	onclick = interaction("next unlabeled segment button clicked", disableUI = true) {
	  switchSegment(next = true, unlabelled = true)
	}
  }

  val completionP = controlsDiv.p {
	innerText = "$completionFraction segments labelled"
  }

  override val nextImageButton =
	controlsDiv.appendWrapper(HoldButton("Submit Responses").withConfig {
	  disabledProperty().bind(finishedProp.not())
	  type = ButtonType.button.realValue
	  sty {
		fontWeight = bold
		boxButton()
	  }
	})


  val helpText = controlsDiv.p {
	hidden = !training
	selectedSegments.onChange {
	  if (selectedSegments.isEmpty() && isNotFinished) phase = UNSELECTED
	  if (selectedSegments.isNotEmpty()) {
		if (selectedSegments.any { it.hasResponse }) {
		  if (isNotFinished) phase = SELECTED_LABELLED
		} else {
		  if (isNotFinished) phase = SELECTED_UNLABELLED
		}
	  }
	}

	val finished =
	  "Great job. You have chosen a label for every segment in this drawing. You can still go back and change your responses before continuing. Once ready, click and hold the \"${nextImageButton.text}\" button. After moving onto the next drawing, you will not be able to come back and change your responses on the current drawing."

	if (PARAMS.allowMultiSelection) {
	  innerHTMLProperty().bind(phaseProp.binding {
		"This first drawing is for training purposes only and the data will not be used.<br><br>" + when (it) {
		  UNSELECTED          -> "You may select one or more segments by clicking them. Normally, clicking a segment will deselect your previous selections. In order to select multiple segments, hold down the SHIFT key. You may also click the \"${nextUnlabeledSegmentButton.innerText}\" button or the \"${previousUnlabeledSegmentButton.innerText}\" button to cycle through unselected segments automatically. This will also deselect your previous selection(s)."
		  SELECTED_UNLABELLED -> "Now that one or more segments are selected, you may choose a label. Please choose the label by clicking the label button that you think best fits the segment(s). If you change your mind after selecting a label you can reselect the segment(s) and change your response. After choosing a label for a segment for the first time, your selection will be cleared and the next unlabelled segment will automatically be selected for you."
		  SELECTED_LABELLED   -> "One or more selected segments are already labelled. You can still change which label you are assigning to these segments by clicking one of the label buttons."
		  FINISHED            -> finished
		}
	  })
	} else {
	  innerHTMLProperty().bind(phaseProp.binding {
		"This first drawing is for training purposes only and the data will not be used.<br><br>" + when (it) {
		  UNSELECTED          -> "You may select a segment by clicking it. You may also click the \"${nextUnlabeledSegmentButton.innerText}\" button or the \"${previousUnlabeledSegmentButton.innerText}\" button to cycle through unselected segments automatically."
		  SELECTED_UNLABELLED -> "Now that a segment is selected, you may choose a label. Please choose the label by clicking the label button that you think best fits this segment. If you change your mind after selecting a label you can reselect the segment and change your response. After choosing a label for a segment for the first time, the next unlabelled segment will automatically be selected for you."
		  SELECTED_LABELLED   -> "This segment is already labelled. You can still change which label you are assigning to this segment by clicking one of the label buttons."
		  FINISHED            -> finished
		}
	  })
	}

  }

}