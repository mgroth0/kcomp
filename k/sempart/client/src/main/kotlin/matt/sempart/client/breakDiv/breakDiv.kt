package matt.sempart.client.breakDiv

import kotlinx.html.ButtonType
import matt.kjs.css.TextAlign.center
import matt.kjs.css.auto
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.button
import matt.kjs.elements.div
import matt.kjs.elements.p
import matt.kjs.setOnClick
import matt.sempart.client.const.WIDTH
//import matt.sempart.client.mainDivClass
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.centerOnWindow
import kotlin.js.Date

val breakDiv by lazy {
  div {
//	classList.add(mainDivClass)
	onlyShowIn(Break)
	sty.centerOnWindow()
	sty.textAlign = center
	p {
	  sty {
		width = WIDTH.px
		margin = auto
		innerHTML = "You may take a break and continue when you are ready."
		button {
		  type = ButtonType.button.realValue
		  sty.margin = auto
		  innerHTML = "Continue"
		  setOnClick {
			ExperimentState.lastInteract = Date.now()
			ExperimentState.onBreak = false
		  }
		}
	  }
	}
  }
}