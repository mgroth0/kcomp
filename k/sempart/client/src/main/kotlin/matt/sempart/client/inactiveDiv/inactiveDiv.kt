package matt.sempart.client.inactiveDiv

import matt.kjs.css.sty
import matt.kjs.elements.div
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.onlyShowIn
//import matt.sempart.client.sty.centerInParent

val inactiveDiv by lazy {
  div {
	//	classList.add(mainDivClass)
	onlyShowIn(Inactive)
//	sty.centerInParent()
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }
}