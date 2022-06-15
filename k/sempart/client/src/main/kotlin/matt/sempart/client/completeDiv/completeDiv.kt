package matt.sempart.client.completeDiv

import matt.kjs.elements.div
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.onlyShowIn

val completeDiv by lazy {
  div {
	onlyShowIn(Complete)
	innerHTML =
	  "Experiment complete. Thank you! Please click this link. It will confirm you have completed the study with Prolific so that you can be paid: <a href=\"https://app.prolific.co/submissions/complete?cc=92B81EA2\">Click here to confirm your completion of this study with Prolific</a>"
  }
}