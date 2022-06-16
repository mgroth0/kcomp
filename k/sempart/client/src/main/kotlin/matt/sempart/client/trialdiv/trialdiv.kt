@file:OptIn(ExperimentalContracts::class)

package matt.sempart.client.trialdiv

//import matt.sempart.client.sty.centerInParent
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
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.HTMLElementWrapper
import matt.kjs.img.draw
import matt.kjs.img.put
import matt.kjs.pixelIndexInTarget
import matt.kjs.props.disabledProperty
import matt.kjs.props.hiddenProperty
import matt.kjs.props.innerHTMLProperty
import matt.kjs.setOnMouseMove
import matt.kjs.showing
import matt.klib.dmap.withStoringDefault
import matt.klib.lang.go
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
import matt.sempart.client.sty.MED_SPACE
import matt.sempart.client.sty.box
import matt.sempart.client.sty.boxButton
import matt.sempart.client.ui.ExperimentScreen
import matt.sempart.client.ui.boxButton
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.events.MouseEvent
import kotlin.contracts.ExperimentalContracts

interface TrialDiv: HTMLElementWrapper<HTMLDivElement> {
  val nextImageButton: HTMLButtonElement
  //  val hoverCanvas: HTMLCanvasElement

  //  val selectCanvas: HTMLCanvasElement
  val helpText: HTMLParagraphElement
}

private val trialsDivs = WeakMap<DrawingTrial, TrialDiv>().withStoringDefault { it.trialDiv() }
val DrawingTrial.div: TrialDiv get() = trialsDivs[this]

@OptIn(ExperimentalContracts::class)
private fun DrawingTrial.trialDiv(): TrialDiv = object: ExperimentScreen(
  //  flex,
  Trial,
  flexDir = row
), TrialDiv {

  fun eventToSeg(e: MouseEvent) = segmentOf(e.pixelIndexInTarget())

  private var zIdx = 0

  private

  val stackDiv = element.div {
	sty {
	  width = WIDTH.px
	  height = HEIGHT.px
	}
	fun stackCanvas(
	  im: HTMLImageElement?,
	  hide: Boolean = true,
	  op: HTMLCanvasElement.()->Unit = {}
	) {
	  canvas {
		width = WIDTH
		height = HEIGHT
		sty {
		  position = absolute
		  zIndex = zIdx++
		}
		im?.go { draw(it) }
		hidden = hide
		op()
	  }
	}
	stackCanvas(loadingIm, hide = false)
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
//	  appendChilds(
//		//		theSeg.labelledCanvas.withConfig {
//		//		  canvasConfig(theSeg.labelledIm)
//		//		},
////		theSeg.selectCanvas.withConfig {
////		  canvasConfig(theSeg.selectIm)
////		},
////		theSeg.selectLabeledCanvas.withConfig {
////		  canvasConfig(theSeg.selectLabeledIm)
////		}
//	  )
	}
	stackCanvas(im = null, hide = false) {
	  setOnMouseMove {
		ExperimentState.interacted()
		hover(eventToSeg(it))
	  }
	  onclick = interaction("click") {
		val seg = eventToSeg(it)
		if (seg == null) clearSelection()
		else if (seg !in selectedSegments) {
		  if (!PARAMS.allowMultiSelection || !it.shiftKey) clearSelection()
		  select(seg)
		}
	  }
	}
  }

  val controlsDiv: HTMLDivElement = element.div {
	sty {
	  marginBottom = MED_SPACE
	  width = WIDTH.px
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
		innerHTML = l
		sty.fontStyle = italic
		onclick = interaction("selected label: $l") {
		  val gotFirstResponse = selectedSegments.filter {
			//			it.labelledCanvas.hidden = false
			val hadNoResponse = it.hasNoResponse
			it.response = l
			hadNoResponse
		  }
//		  redraw()
		  completionP.innerHTML = "$completionFraction segments labelled"
		  //		  gotFirstResponse.forEach {
		  //			it.showAsLabeled()
		  //		  }
		  if (gotFirstResponse.isNotEmpty()) nextSeg()
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

	val finished =
	  "Great job. You have chosen a label for every segment in this drawing. You can still go back and change your responses before continuing. Once ready, click the \"${nextImageButton.innerHTML}\" button. After moving onto the next drawing, you will not be able to come back and change your responses on the current drawing."

	if (PARAMS.allowMultiSelection) {
	  innerHTMLProperty().bind(phaseProp.binding {
		"This first drawing is for training purposes only and the data will not be used.<br><br>" + when (it) {
		  UNSELECTED          -> "You may select one or more segments by clicking them. Normally, clicking a segment will deselect your previous selections. In order to select multiple segments, hold down the SHIFT key. You may also click the \"${nextUnlabeledSegmentButton.innerHTML}\" button or the \"${previousUnlabeledSegmentButton.innerHTML}\" button to cycle through unselected segments automatically. This will also deselect your previous selection(s)."
		  SELECTED_UNLABELLED -> "Now that one or more segments are selected, you may choose a label. Please choose the label by clicking the label button that you think best fits the segment(s). If you change your mind after selecting a label you can reselect the segment(s) and change your response. After choosing a label for a segment for the first time, your selection will be cleared and the next unlabelled segment will automatically be selected for you."
		  SELECTED_LABELLED   -> "One or more selected segments are already labelled. You can still change which label you are assigning to these segments by clicking one of the label buttons."
		  FINISHED            -> finished
		}
	  })
	} else {
	  innerHTMLProperty().bind(phaseProp.binding {
		"This first drawing is for training purposes only and the data will not be used.<br><br>" + when (it) {
		  UNSELECTED          -> "You may select a segment by clicking it. You may also click the \"${nextUnlabeledSegmentButton.innerHTML}\" button or the \"${previousUnlabeledSegmentButton.innerHTML}\" button to cycle through unselected segments automatically."
		  SELECTED_UNLABELLED -> "Now that a segment is selected, you may choose a label. Please choose the label by clicking the label button that you think best fits this segment. If you change your mind after selecting a label you can reselect the segment and change your response. After choosing a label for a segment for the first time, the next unlabelled segment will automatically be selected for you."
		  SELECTED_LABELLED   -> "This segment is already labelled. You can still change which label you are assigning to this segment by clicking one of the label buttons."
		  FINISHED            -> finished
		}
	  })
	}

  }

}