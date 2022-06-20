package matt.v1.gui.fig.axes

import de.gsi.chart.axes.spi.AxisRange
import de.gsi.chart.axes.spi.DefaultNumericAxis
import javafx.scene.paint.Color
import matt.v1.figmodels.AxisConfig

class ControlledNumericAxis(cfg: AxisConfig): DefaultNumericAxis(cfg.label) {
  init {
	if (cfg.min != null) {
	  this.min = cfg.min.toDouble()
	}
	if (cfg.max != null) {
	  this.max = cfg.max.toDouble()
	}
	this.unit = cfg.unit?.longNamePlural
	axisLabel.apply {
	  text = cfg.label
	  fill = Color.WHITE
	}
  }

  var controlledTickValues: MutableList<Double>? = null
  override fun calculateMajorTickValues(axisLength: Double, axisRange: AxisRange?): MutableList<Double> {
	return controlledTickValues ?: super.calculateMajorTickValues(axisLength, axisRange)
  }
}