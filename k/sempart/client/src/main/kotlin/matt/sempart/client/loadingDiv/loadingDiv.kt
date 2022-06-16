package matt.sempart.client.loadingDiv

import matt.kjs.node.LoadingProcess

//import matt.sempart.client.mainDivClass
//Zimport matt.kjs.node.LoadingProcess
import matt.sempart.client.state.ExperimentPhase.Loading
import matt.sempart.client.state.ExperimentState
import matt.sempart.client.ui.ExperimentScreen

//import matt.sempart.client.sty.centerInParent

object LoadingDiv: ExperimentScreen(Loading) {
  val lt = loadingText("Loading") {
	//	element.classList.add(mainDivClass)
	//	onlyShowIn(Loading)
	//	element.sty.centerInParent()
	//	element.sty{
	//	  display = flex
	//	  justifyContent = JustifyContent.center
	//	  alignItems = AlignItems.center
	//	  flexDirection = column
	//	}
  }
}


class DrawingLoadingProcess(desc: String): LoadingProcess(LoadingDiv.lt, desc) {
  override fun start() {
	ExperimentState.working = true
	super.start()
  }

  override fun <R> finish(message: String?, op: ()->R): R {
	ExperimentState.working = false
	return super.finish(message, op)
  }
}