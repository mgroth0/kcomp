package matt.stim.flicker

import javafx.animation.Animation.INDEFINITE
import javafx.animation.Interpolator
import javafx.application.Platform.runLater
import javafx.scene.CacheHint
import javafx.scene.paint.Color
import javafx.util.Duration
import matt.gui.app.GuiApp
import matt.hurricanefx.tornadofx.animation.keyframe
import matt.hurricanefx.tornadofx.animation.timeline
import matt.hurricanefx.tornadofx.shapes.circle
import matt.hurricanefx.wrapper.wrapped

const val HZ = 10.0
const val PER_SEC = 1/HZ
const val HALF_PER_SEC = PER_SEC/2

fun main() = GuiApp {



  rootPane {
	wrapped().circle(centerX = 300.0, centerY = 300.0, radius = 50.0) {
	  isCache = true
	  cacheHint = CacheHint.SPEED
	  fill = Color.WHITE
	  runLater {
		timeline {
		  cycleCount = INDEFINITE
		  keyframe(Duration.seconds(HALF_PER_SEC)) {
			keyvalue(this@circle.visibleProperty(), false, Interpolator.DISCRETE)
			/*keyvalue(this@circle.fillProperty(), matt.css.Color.BLACK, Interpolator.DISCRETE)*/
		  }
		  keyframe(Duration.seconds(PER_SEC)) {
			keyvalue(this@circle.visibleProperty(), true, Interpolator.DISCRETE)
			/*keyvalue(this@circle.fillProperty(), matt.css.Color.WHITE, Interpolator.DISCRETE)*/
		  }
		}
	  }
	}
  }
  stage.apply {
	width = 600.0
	height = 600.0
	centerOnScreen()
  }
}.start()