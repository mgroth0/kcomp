package matt.sempart.client.loadingDiv

import matt.kjs.html.elements.loadingText
import matt.kjs.node.LoadingProcess
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.ui.ExperimentScreen

private val lt = loadingText("Loading")
val loadingDiv = ExperimentScreen(Loading) {
  +lt
}


class DrawingLoadingProcess(desc: String): LoadingProcess(lt, desc) {
  override fun start() {
	ExperimentState.working.value = true
	super.start()
  }

  override fun <R> finish(message: String?, op: ()->R): R {
	ExperimentState.working.value = false
	return super.finish(message, op)
  }
}