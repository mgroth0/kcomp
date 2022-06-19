package matt.sempart.client.ui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.ButtonType
import kotlinx.html.div
import kotlinx.html.dom.create
import matt.klib.css.AlignItems
import matt.klib.css.Display
import matt.klib.css.Display.flex
import matt.klib.css.Display.none
import matt.klib.css.FlexDirection
import matt.klib.css.FlexDirection.column
import matt.klib.css.Px
import matt.klib.css.percent
import matt.klib.css.px
import matt.kjs.css.sty
import matt.kjs.html.elements.AwesomeElement
import matt.kjs.html.elements.HTMLElementWrapper
import matt.kjs.html.elements.WithDefaultDisplay
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.klib.prop.BindableProperty
import matt.klib.prop.ReadOnlyBindableProperty
import matt.klib.css.JustifyContent
import matt.sempart.client.breakDiv.breakDiv
import matt.sempart.client.completeDiv.completeDiv
import matt.sempart.client.const.HALF_WIDTH
import matt.sempart.client.errorDiv.errorDiv
import matt.sempart.client.inactiveDiv.inactiveDiv
import matt.sempart.client.instructionsDiv.instructionsDiv
import matt.sempart.client.instructionsDiv.instructionsVid.instructionsVidDiv
import matt.sempart.client.loadingDiv.loadingDiv
import matt.sempart.client.nameDiv.nameDiv
import matt.sempart.client.resizeDiv.resizeDiv
import matt.sempart.client.scaleDiv.DEFAULT_SCALE
import matt.sempart.client.scaleDiv.scaleDiv
import matt.sempart.client.scaleDiv.scaleProp
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.listen
import org.w3c.dom.HTMLDivElement

val SCREENS by lazy {
  val list = listOf(
	scaleDiv,
	nameDiv,
	instructionsVidDiv,
	instructionsDiv,
	resizeDiv,
	loadingDiv,
	completeDiv,
	breakDiv,
	inactiveDiv,
	errorDiv
  )
  require(list.size == ExperimentPhase.values().size - 1) {
	"did you forget to add a new div to the div list?"
  }
  scaleProp.onChange { scale ->
	list.forEach {
	  it.sty.resetTransform {
		scale(scale)
	  }
	}
  }
  list.onEach {
	it.sty.resetTransform {
	  scale(DEFAULT_SCALE)
	}
  }
}

open class ExperimentScreen(
  phase: ExperimentPhase,
  override val defaultDisplay: Display = flex,
  val flexDir: FlexDirection = column,
  cfg: ExperimentScreen.()->Unit = {}
): AwesomeElement<HTMLDivElement>(), WithDefaultDisplay<HTMLDivElement> {
  final override val element by lazy {
	document.create.div {
	  sty {
		display = defaultDisplay
		justifyContent = JustifyContent.center
		alignItems = AlignItems.center
		flexDirection = flexDir
		height = 100.percent
	  }
	} as HTMLDivElement
  }

  init {
	onlyShowIn(phase)
  }

  init {
	cfg()
  }
}

fun HTMLElementWrapper<HTMLDivElement>.boxButton(op: HTMLButtonWrapper.()->Unit = {}) = button {
  type = ButtonType.button.realValue
//  sty.boxButton()

  op()
}


val currentLeftProp: ReadOnlyBindableProperty<Px> = BindableProperty(currentLeft().px).apply {
  window.addEventListener("resize", {
	value = currentLeft().px
  })
}

//object MyResizeLeft: ChangeEventDispatcher<Int>() {
//
//}

//fun AwesomeElement<*>.onlyShowIn(phase: ExperimentPhase) = element.onlyShowIn(phase)


fun WithDefaultDisplay<*>.onlyShowIn(phase: ExperimentPhase) {
  if (ExperimentPhase.determine() != phase) {
	sty.display = none
  }
  listen(PhaseChange) {
	if (it.second == phase) {
	  if (sty.display == none) {
		sty.display = defaultDisplay
	  }
	} else sty.display = none
  }
}

fun currentLeft() = (window.innerWidth/2) - HALF_WIDTH
//fun HTMLElement.onMyResizeLeft(onLeft: (Int)->Unit) {
//  onLeft(currentLeft())
//  listen(MyResizeLeft) {
//	onLeft(it)
//  }
//}