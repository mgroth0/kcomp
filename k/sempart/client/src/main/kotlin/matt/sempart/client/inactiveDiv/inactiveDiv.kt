package matt.sempart.client.inactiveDiv

import matt.kjs.elements.div
import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.state.onlyShowIn

val inactiveDiv by lazy {
  div {
	onlyShowIn(Inactive)
	innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
  }
}