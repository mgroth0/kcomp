package matt.sempart.client.ui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.ButtonType
import kotlinx.html.div
import kotlinx.html.dom.create
import matt.kjs.css.AlignItems
import matt.kjs.css.Display
import matt.kjs.css.Display.flex
import matt.kjs.css.Display.none
import matt.kjs.css.FlexDirection
import matt.kjs.css.FlexDirection.column
import matt.kjs.css.JustifyContent
import matt.kjs.css.Px
import matt.kjs.css.percent
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.html.elements.AwesomeElement
import matt.kjs.html.elements.WithDefaultDisplay
import matt.kjs.html.elements.button
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.kjs.prop.BindableProperty
import matt.kjs.prop.ReadOnlyBindableProperty
import matt.sempart.client.breakDiv.breakDiv
import matt.sempart.client.completeDiv.completeDiv
import matt.sempart.client.const.HALF_WIDTH
import matt.sempart.client.errorDiv.errorDiv
import matt.sempart.client.inactiveDiv.inactiveDiv
import matt.sempart.client.instructionsDiv.instructionsDiv
import matt.sempart.client.instructionsDiv.instructionsVid.instructionsVidDiv
import matt.sempart.client.loadingDiv.LoadingDiv
import matt.sempart.client.resizeDiv.resizeDiv
import matt.sempart.client.scaleDiv.DEFAULT_SCALE
import matt.sempart.client.scaleDiv.scaleDiv
import matt.sempart.client.scaleDiv.scaleProp
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.listen
import matt.sempart.client.sty.boxButton
import org.w3c.dom.HTMLDivElement

val SCREENS by lazy {
  val list = listOf(
	scaleDiv,
	instructionsVidDiv,
	instructionsDiv,
	resizeDiv,
	LoadingDiv,
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
		scale(scale.toDouble())
	  }
	}
  }
  list.onEach {
	it.sty.resetTransform {
	  scale(DEFAULT_SCALE.toDouble())
	}
  }
}

open class ExperimentScreen(
  phase: ExperimentPhase,
  override val defaultDisplay: Display = flex,
  val flexDir: FlexDirection = column,
  val cfg: ExperimentScreen.()->Unit = {}
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

fun HTMLDivElement.boxButton(op: HTMLButtonWrapper.()->Unit = {}) = button {
  type = ButtonType.button.realValue
  sty.boxButton()
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