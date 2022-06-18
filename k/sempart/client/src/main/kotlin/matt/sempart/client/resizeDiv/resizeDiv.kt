package matt.sempart.client.resizeDiv

import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.sempart.client.const.HEIGHT
import matt.sempart.client.const.WIDTH
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.ui.ExperimentScreen


val resizeDiv = ExperimentScreen(Resize) {
  p {
	sty.margin = auto
	+"Your window is too small. Please enlarge the browser window so the width is at least $WIDTH pixels and the height is at least $HEIGHT pixels."
  }
}