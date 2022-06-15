package matt.sempart.client.loadingDiv

import matt.kjs.elements.loadingText
import matt.kjs.node.LoadingProcess
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn

val loadingDiv by lazy {
  loadingText("Loading") {
	element.classList.add("mainDiv")
	onlyShowIn(Loading)
  }
}


class DrawingLoadingProcess(desc: String): LoadingProcess(loadingDiv, desc) {
  override fun start() {
	ExperimentState.working = true
	super.start()
  }

  override fun <R> finish(message: String?, op: ()->R): R {
	ExperimentState.working = false
	return super.finish(message, op)
  }
}