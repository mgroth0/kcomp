package matt.sempart.client.resizeDiv

import matt.kjs.css.auto
import matt.kjs.css.sty
import matt.kjs.elements.div
import matt.kjs.elements.p
import matt.sempart.client.state.ExperimentPhase.Resize
import matt.sempart.client.state.onlyShowIn

val resizeDiv by lazy {
  div {
	classList.add(mainDivClass)
	onlyShowIn(Resize)
	p {
	  sty.margin = auto
	  innerHTML =
		"Your window is too small. Please enlarge the browser window so it is at least matt.sempart.client.const.getWidth=900 by matt.sempart.client.const.getHeight=750."
	}
  }
}