package matt.sempart.client.completeDiv

import matt.kjs.handlers.setOnClick
import matt.kjs.html.elements.button.HTMLButtonWrapper
import matt.kjs.props.valueProperty
import matt.sempart.Feedback
import matt.sempart.client.const.COMPLETION_URL
import matt.sempart.client.state.ExperimentPhase.Complete
import matt.sempart.client.state.sendData
import matt.sempart.client.ui.ExperimentScreen

var debugButton: HTMLButtonWrapper? = null

val completeDiv = ExperimentScreen(Complete) {
  +"The experiment is complete. Thank you for your participation!"

  br
  br

  var b: HTMLButtonWrapper? = null

  +"Optionally, if you would like to give the researchers feedback on this experiment please submit it here."
  println("adding textarea")
  val ta = textArea {
	println("configuring textarea")
	valueProperty().onChange {
	  b!!.enabled = true
	}
	println("configured textarea")
  }
  println("added textarea")

  println("creating button")
  b = button {
//	println("innerHTML3=${debugButton?.innerHTML}")
	println("configuring button")
	+"Submit Feedback"
	setOnClick {
	  enabled = false
	  sendData(Feedback(ta.value))
	}
//	println("configured button (innerHTML=$innerHTML)")
//	println("innerHTML4=${debugButton?.innerHTML}")
  }
  debugButton = b
  b.id = "debugButton"
  println("created button")
//  println("innerHTML1=${debugButton?.innerHTML}")

  +"To confirm your completion of the study with Prolific (which necessary for payment) please "
  println("creating a")
  a {
	println("configuring a")
	href = COMPLETION_URL
	+"click here"
	println("configured a")
  }
  println("created a")
//  println("innerHTML2=${debugButton?.innerHTML}")
}