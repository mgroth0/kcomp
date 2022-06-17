package matt.sempart.client.inactiveDiv

import matt.sempart.client.state.ExperimentPhase.Inactive
import matt.sempart.client.ui.ExperimentScreen

val inactiveDiv = ExperimentScreen(Inactive) {
  element.innerHTML = "Sorry, you have been inactive for too long and the experiment has been cancelled."
}