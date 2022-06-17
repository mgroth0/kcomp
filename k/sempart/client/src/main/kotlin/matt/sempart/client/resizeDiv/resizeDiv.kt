package matt.sempart.client.resizeDiv

//import matt.sempart.client.mainDivClass
import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.ui.ExperimentScreen

//import matt.sempart.client.sty.centerInParent

val resizeDiv = ExperimentScreen(Resize) {
  //  div {
  //	classList.add(mainDivClass)
  //	onlyShowIn(Resize)
  //	sty{
  //	  display = flex
  //	  justifyContent = JustifyContent.center
  //	  alignItems = AlignItems.center
  //	  flexDirection = column
  //	}
  //	sty.centerInParent()
  p {
	sty.margin = auto
	+"Your window is too small. Please enlarge the browser window so it is at least matt.sempart.client.const.getWidth=900 by matt.sempart.client.const.getHeight=750."
  }
  //  }
}