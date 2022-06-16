package matt.sempart.client.loadingDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.sty
import matt.kjs.elements.loadingText
import matt.kjs.node.LoadingProcess
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.state.onlyShowIn
import matt.sempart.client.sty.centerOnWindow

val loadingDiv by lazy {
  loadingText("Loading") {
//	element.classList.add(mainDivClass)
	onlyShowIn(Loading)
	element.sty.centerOnWindow()
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