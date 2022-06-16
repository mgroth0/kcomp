package matt.sempart.client.inactiveDiv

import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.ui.ExperimentScreen

//import matt.sempart.client.state.onlyShowIn

//import matt.sempart.client.sty.centerInParent

val inactiveDiv = ExperimentScreen(Inactive).apply {
  div {    //	classList.add(mainDivClass)
	//	onlyShowIn(Inactive)
	//	sty {
	//	  display = flex
	//	  justifyContent = JustifyContent.center
	//	  alignItems = AlignItems.center
	//	  flexDirection = column
	//	}
	//	sty.centerInParent()
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }
}