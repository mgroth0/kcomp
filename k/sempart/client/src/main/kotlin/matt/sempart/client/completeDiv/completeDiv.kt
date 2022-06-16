package matt.sempart.client.completeDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.AlignItems
import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.column
import matt.kjs.css.JustifyContent
import matt.kjs.css.sty
import matt.kjs.elements.div
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.onlyShowIn
//import matt.sempart.client.sty.centerInParent

val completeDiv by lazy {
  div {
//	classList.add(mainDivClass)
	onlyShowIn(Complete)
//	sty.centerInParent()
	sty{
	  display = flex
	  justifyContent = JustifyContent.center
	  alignItems = AlignItems.center
	  flexDirection = column
	}
	innerHTML =
	  "Experiment complete. Thank you! Please click this link. It will confirm you have completed the study with Prolific so that you can be paid: <a href=\"https://app.prolific.co/submissions/complete?cc=92B81EA2\">Click here to confirm your completion of this study with Prolific</a>"
  }
}