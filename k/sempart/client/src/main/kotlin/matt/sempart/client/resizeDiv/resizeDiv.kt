package matt.sempart.client.resizeDiv

import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.sempart.client.scaleDiv.neededHeight
import matt.sempart.client.scaleDiv.neededWidth
import matt.sempart.client.scaleDiv.scaleProp
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.ui.ExperimentScreen



val resizeDiv = ExperimentScreen(Resize) {
  p {
	sty.margin = auto

	+"Your window is too small. Please enlarge the browser window so the width is at least ${neededWidth()} pixels and the height is at least ${neededHeight()} pixels."
	scaleProp.onChange {
	  clear()
	  +"Your window is too small. Please enlarge the browser window so the width is at least ${neededWidth()} pixels and the height is at least ${neededHeight()} pixels."
	}

  }
}