package matt.v1.model.combined

import matt.kjlib.jmath.div
import matt.kjlib.jmath.times
import matt.klib.math.BasicPoint
import matt.v1.lab.petri.PopulationConfig
import matt.v1.lab.petri.rosenbergPop
import matt.v1.low.PhaseType.SIN
import matt.v1.model.Cell
import matt.v1.model.Orientation
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import matt.v1.model.activity.ARI_BASE_ACTIVITY_CFG
import matt.v1.model.activity.ActivityConfig
import matt.v1.model.field.FieldConfig
import matt.v1.model.stim.StimulusConfig
import matt.v1.model.stim.rosenbergBaseStimConfig

data class CombinedConfig(
  val fieldConfig: FieldConfig,
  val stimConfig: StimulusConfig,
  val populationConfig: PopulationConfig,
  val activityConfig: ActivityConfig
) {

  private val baseLoc = BasicPoint(x = -populationConfig.cellX0AbsMinmax, 0.0)
  private val baseOrientation = Orientation(0.0)
  val baseStim by lazy {
	Stimulus(
	  fieldCfg = fieldConfig,
	  //	  f = baseField,
	  a = stimConfig.baseContrast,
	  s = stimConfig.stimSigma,
	  SF = stimConfig.stimSF,
	  fieldLoc = baseLoc,
	  o = baseOrientation
	)
  }
  val baseSimpleSinCell by lazy {
	SimpleCell(
	  fieldCfg = fieldConfig,
	  sx = (if (populationConfig.proportionalCellSigmas) (populationConfig.baseSx*populationConfig.cellSFmin)/populationConfig.cellSFmin/*thisCellSF*/ else populationConfig.baseSx).toDouble(),
	  sy = (if (populationConfig.proportionalCellSigmas) (populationConfig.baseSy*populationConfig.cellSFmin)/populationConfig.cellSFmin/*thisCellSF*/ else populationConfig.baseSy).toDouble(),    /*Rosenberg used an elliptical receptive field without explanation. This may interfere with some orientation-based results*/    /*sx = 0.95,
	  sy = 0.95,*/

	  SF = populationConfig.cellSFmin.toDouble(),
	  phase = SIN,
	  fieldLoc = baseLoc,
	  o = baseOrientation

	)
  }

  fun perfectStimFor(cell: Cell) = baseStim.copy(
	o = cell.o,
	fieldLoc = cell.fieldLoc,
	SF = cell.SF
  )
}

val ARI_BASE_CFG = CombinedConfig(
  activityConfig = ARI_BASE_ACTIVITY_CFG,
  fieldConfig = FieldConfig(),
  stimConfig = rosenbergBaseStimConfig,
  populationConfig = rosenbergPop
)