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
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.AwesomeElement
import matt.kjs.elements.WithDefaultDisplay
import matt.kjs.elements.button
import matt.kjs.prop.BindableProperty
import matt.kjs.prop.ReadOnlyBindableProperty
import matt.sempart.client.const.HALF_WIDTH
import matt.sempart.client.state.ExperimentPhase
import matt.sempart.client.state.PhaseChange
import matt.sempart.client.state.listen
import matt.sempart.client.sty.boxButton
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

open class ExperimentScreen(
  phase: ExperimentPhase,
  override val defaultDisplay: Display = flex,
  val flexDir: FlexDirection = column,
  val cfg: ExperimentScreen.()->Unit = {}
): AwesomeElement<HTMLDivElement>(), WithDefaultDisplay<HTMLDivElement> {
  final override val element by lazy {
	document.create.div {
	  sty {
		justifyContent = JustifyContent.center
		alignItems = AlignItems.center
		flexDirection = flexDir
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

fun HTMLDivElement.boxButton(op: HTMLButtonElement.()->Unit = {}) = button {
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