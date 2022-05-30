package matt.v1.figmodels

import matt.klib.math.Point
import matt.klib.math.UnitType
import matt.v1.activity.Response
import matt.v1.comp.Fit
import matt.v1.comp.PoissonVar
import matt.v1.comp.PoissonVar.NONE
import matt.v1.lab.Experiment.CoreLoop
import matt.v1.model.Stimulus
import matt.v1.model.complexcell.ComplexCell
import org.apfloat.Apfloat

interface SeriesCfgs {
  val label: String
  val line: Boolean
  val markers: Boolean
}

data class SeriesCfg(
  val priorWeight: Apfloat? = null,
  val yExtractCustom: (CoreLoop.()->Number)? = null,
  val poissonVar: PoissonVar = NONE,
  val popRcfg: (Map<ComplexCell,Response>.()->Map<ComplexCell,Response>) = { this },
  val stimCfg: CoreLoop.(Stimulus)->Stimulus = { it.copy() },
  val fit: Fit? = null,
  override val label: String,
  override val line: Boolean = true,
  override val markers: Boolean = false,
): SeriesCfgs

data class SeriesCfgV2(
  override val label: String,
  override val line: Boolean = true,
  override val markers: Boolean = false,
): SeriesCfgs


data class AxisConfig(
  val min: Number?,
  val max: Number?,
  val unit: UnitType?,
  val label: String,
  val postProcessOp: ((List<Point>)->(List<Point>)) = { it }
)