package matt.v1.lab.petri

import matt.kjlib.ranges.step
import matt.klibexport.klibexport.allUnique
import matt.v1.compcache.Point
import matt.v1.compcache.Polar
import matt.v1.compcache.Radians
import matt.v1.model.Cell
import matt.v1.model.Circle
import matt.v1.model.ComplexCell
import matt.v1.model.Field
import matt.v1.model.FieldLocAndOrientation
import matt.v1.model.SimpleCell
import matt.v1.model.SimpleCell.Phase
import matt.v1.model.SimpleCell.Phase.SIN
import matt.v1.model.Stimulus
import matt.v1.model.ZERO


data class PopulationConfig(

  val cellX0AbsMinmax: Float = 15.0.toFloat(),
  val cellX0Step: Float = 0.2.toFloat(),
  val cellX0StepMult: Int = 1,
  val prefThetaMin: Float = 0.0.toFloat(),
  val prefThetaMax: Float = 179.0.toFloat(),
  val popCCSpacingThetaStep: Float = 90.0.toFloat(),
  val reqSize: Int? = null, /*27_180=rosenberg*/ /*sanity*/
  val cellPrefThetaStep: Float = 1.0.toFloat(),
  val stimRecCCThetaStep: Float = 90.0.toFloat(),
  val matCircles: Boolean = false,
  val alongY: Boolean = false,
  val conCircles: Boolean = false,

  ) {

  val FIELD_SIZE_MULT = 1/*complete guess*/
  /*val FIELD_SAMPLE_STEP_MULT = 1*/

  val fieldAbsMinMax = cellX0AbsMinmax*FIELD_SIZE_MULT


  val baseField by lazy {
	FieldLocAndOrientation(
	  t = ZERO,
	  X0 = -cellX0AbsMinmax,
	  Y0 = ZERO,
	  field = Field(
		absLimXY = fieldAbsMinMax,
		stepXY = 0.2f /*rosenberg=0.2*/ /*cellX0Step * FIELD_SAMPLE_STEP_MULT*/ /*complete guess*/
	  )
	)
  }


  val baseStim by lazy {
	Stimulus(
	  popCfg = this,
	  f = baseField,
	  a = 0.5f,
	  s = 1.55f,
	  SF = 5.75f,
	)
  }


  val baseSimpleSinCell by lazy {
	SimpleCell(
	  popCfg = this,
	  f = baseField,


	  sx = 0.7f,
	  sy = 1.2f,
	  /*Rosenberg used an elliptical receptive field without explanation. This may interfere with some orientation-based results*/
	  /*sx = 0.95,
	  sy = 0.95,*/

	  SF = 4.0f,
	  phase = SIN
	)
  }

  fun perfectStimFor(cell: Cell) = baseStim.copy(
	f = baseStim.f.copy(t = cell.t, X0 = cell.X0, Y0 = cell.Y0)
  )
}


class Population(
  val cfg: PopulationConfig,
) {
  init {
	println("building pop")
	require(!(cfg.alongY && cfg.conCircles))
  }

  val centralCell by lazy {
	complexCells
	  .filter { it.t == 0.0f }
	  .filter {
		it.X0 >= 0.0 && it.Y0 >= 0.0
	  }
	  .minByOrNull { it.X0 + it.Y0 }!!
  }


  private val spacialRange =
	(-cfg.cellX0AbsMinmax..cfg.cellX0AbsMinmax step (cfg.cellX0Step*cfg.cellX0StepMult))
  private val circle = Circle(radius = cfg.cellX0AbsMinmax)
  private val sinCells = spacialRange
	.flatMapIndexed { circI, x0 ->
	  (cfg.prefThetaMin..cfg.prefThetaMax step (cfg.cellPrefThetaStep)).flatMap { t ->
		if (cfg.conCircles) {
		  when {
			x0 < 0.0   -> listOf()
			x0 == 0.0f -> mutableListOf(
			  cfg.baseSimpleSinCell.copy(
				f = cfg.baseSimpleSinCell.f.copy(
				  X0 = 0.0f,
				  Y0 = 0.0f,
				  t = t
				)
			  )
			)
			else       -> {
			  val cells =
				mutableListOf<SimpleCell>()
			  val circleThetaStep = (cfg.popCCSpacingThetaStep)/*90*//(circI + 1)
			  var a = 0.0f
			  while (a < 360.0) {
				val p = Polar(x0, Radians(a))()
				cells += cfg.baseSimpleSinCell.copy(f = cfg.baseSimpleSinCell.f.copy(X0 = p.x, Y0 = p.y, t = t))
				a += circleThetaStep
			  }
			  cells
			}
		  }
		} else {
		  (if (cfg.alongY) spacialRange else listOf(0.0f)).flatMap { y0 ->
			listOf(cfg.baseSimpleSinCell.copy(f = cfg.baseSimpleSinCell.f.copy(X0 = x0, Y0 = y0, t = t)))
		  }
		}
	  }


	}.apply {
	}.filter {
	  cfg.conCircles ||
		  Point(x = it.X0, y = it.Y0).normDist(circle.center) <= circle.radius
	}

	.also {
	  require(it.allUnique())
	  if (cfg.reqSize != null) {
		require(it.size == cfg.reqSize) { "size is ${it.size} but should be ${cfg.reqSize}" }
	  }
	}


  private val cosCells = sinCells.map {
	it.copy(phase = Phase.COS)
  }


  val complexCells = sinCells.zip(cosCells).map { ComplexCell(it) }
}


val pop2D = PopulationConfig(
  conCircles = true,
  cellX0AbsMinmax = 3.0f,
  cellX0Step = 1.0f,
  cellPrefThetaStep = 30.0f,
  stimRecCCThetaStep = 30.0f,
)
val popLouie = pop2D.copy(
  cellX0AbsMinmax = 0.0f,
  prefThetaMax = 90.0f,
  popCCSpacingThetaStep = 500.0f,
  cellPrefThetaStep = 500.0f,
  reqSize = 1
)
val popLouieMoreCells = popLouie.copy(
  conCircles = true,
  cellX0AbsMinmax = 1.0f,
  prefThetaMax = 10.0f,
  popCCSpacingThetaStep = 90.0f,
  reqSize = 5
)
val popLouieFullThetaCells = popLouieMoreCells.copy(
  prefThetaMax = 179.0f,
  cellPrefThetaStep = 1.0f,
  reqSize = 180*5
)