package matt.sempart.client.inactiveDiv

import matt.kjs.css.AlignItems
import matt.kjs.css.Display.flex
import matt.kjs.css.FlexDirection.column
import matt.kjs.css.JustifyContent
import matt.kjs.css.sty
import matt.kjs.elements.div
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.onlyShowIn

//import matt.sempart.client.sty.centerInParent

val inactiveDiv by lazy {
  div {
	//	classList.add(mainDivClass)
	onlyShowIn(Inactive)
	sty {
	  display = flex
	  justifyContent = JustifyContent.center
	  alignItems = AlignItems.center
	  flexDirection = column
	}
	//	sty.centerInParent()
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }
}