package matt.sempart.client.breakDiv

//import matt.sempart.client.mainDivClass
//import matt.sempart.client.sty.centerInParent
import kotlinx.html.ButtonType
import matt.kjs.css.auto
import matt.kjs.css.px
import matt.kjs.css.sty
import matt.kjs.elements.button
import matt.kjs.setOnClick
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Break
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.ui.ExperimentScreen
import kotlin.js.Date


val breakDiv = ExperimentScreen(
  Break
) {
  //  div {
  //	classList.add(mainDivClass)
  //	onlyShowIn(Break)
  //	sty.centerInParent()
  ////	sty.textAlign = center
  //	sty{
  ////	  display = flex
  ////	  justifyContent = center
  ////	  alignItems = AlignItems.center
  //	  flexDirection = column
  //	}
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
  //  }
}