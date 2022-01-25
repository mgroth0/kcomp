package matt.v1.lab.petri

import matt.kjlib.ranges.step
import matt.klibexport.klibexport.allUnique
import matt.v1.compcache.Point
import matt.v1.compcache.Polar
import matt.v1.compcache.Radians
import matt.v1.lab.rcfg.ResourceUsageCfg.FINAL
import matt.v1.lab.rcfg.rCfg
import matt.v1.model.Cell
import matt.v1.model.Circle
import matt.v1.model.ComplexCell
import matt.v1.model.Field
import matt.v1.model.FieldLocAndOrientation
import matt.v1.model.SimpleCell
import matt.v1.model.SimpleCell.Phase
import matt.v1.model.SimpleCell.Phase.SIN
import matt.v1.model.Stimulus

val baseField = FieldLocAndOrientation(
  t = rCfg.THETA_MIN,
  X0 = -rCfg.X0_ABS_MINMAX,
  Y0 = 0.0,
  field = Field(absLimXY = rCfg.FIELD_ABS_MINMAX, stepXY = rCfg.X0_STEP)/*complete guess*/
)


val baseStim = Stimulus(
  f = baseField,
  a = 0.5,
  s = 1.55,
  SF = 5.75,
)


val baseSimpleSinCell = SimpleCell(
  f = baseField,


  sx = 0.7,
  sy = 1.2,
  /*Rosenberg used an elliptical receptive field without explanation. This may interfere with some orientation-based results*/
  /*sx = 0.95,
  sy = 0.95,*/

  SF = 4.0,
  phase = SIN
)


fun Cell.perfectStim() = baseStim.copy(f = baseStim.f.copy(t = t, X0 = X0, Y0 = Y0))


class Population(
  alongY: Boolean = false,
  conCircles: Boolean = false,
  val X0_ABS_MINMAX: Double = rCfg.X0_ABS_MINMAX,
  val X0_STEP: Double = rCfg.X0_STEP,
  val CELL_X0_STEP_MULT: Int = rCfg.CELL_X0_STEP_MULT,
  val THETA_MIN: Double = rCfg.THETA_MIN,
  val THETA_MAX: Double = rCfg.THETA_MAX,
  val CELL_THETA_STEP: Double = rCfg.CELL_THETA_STEP,
  val REQ_SIZE: Int? = if (rCfg == FINAL) rCfg.REQ_SIZE else null
) {
  init {
	println("building pop")
	require(!(alongY && conCircles))
  }

  val centralCell by lazy {
	complexCells
	  .filter { it.t == 0.0 }
	  .filter {
		it.X0 >= 0.0 && it.Y0 >= 0.0
	  }
	  .minByOrNull { it.X0 + it.Y0 }!!
  }


  private val spacialRange =
	(-X0_ABS_MINMAX..X0_ABS_MINMAX step (X0_STEP*CELL_X0_STEP_MULT))
  private val circle = Circle(radius = X0_ABS_MINMAX)
  private val sinCells = spacialRange
	.flatMap { x0 ->
	  (THETA_MIN..THETA_MAX step (CELL_THETA_STEP)).flatMap { t ->
		if (conCircles) {
		  when {
			x0 < 0.0  -> listOf()
			x0 == 0.0 -> mutableListOf(
			  baseSimpleSinCell.copy(
				f = baseSimpleSinCell.f.copy(
				  X0 = 0.0,
				  Y0 = 0.0,
				  t = t
				)
			  )
			)
			else      -> {
			  val cells =
				mutableListOf<SimpleCell>()
			  val circleThetaStep = (CELL_THETA_STEP)/*90*//x0
			  var a = 0.0
			  while (a < 360.0) {
				val p = Polar(x0, Radians(a))()
				cells += baseSimpleSinCell.copy(f = baseSimpleSinCell.f.copy(X0 = p.x, Y0 = p.y, t = t))
				a += circleThetaStep
			  }
			  cells
			}
		  }
		} else {
		  (if (alongY) spacialRange else listOf(0.0)).flatMap { y0 ->
			listOf(baseSimpleSinCell.copy(f = baseSimpleSinCell.f.copy(X0 = x0, Y0 = y0, t = t)))
		  }
		}
	  }


	}.apply {
	}.filter {
	  if (conCircles) true
	  else Point(x = it.X0, y = it.Y0).normDist(circle.center) <= circle.radius
	}

	.also {
	  require(it.allUnique())
	  if (REQ_SIZE != null) {
		require(it.size == REQ_SIZE) { "size is ${it.size} but should be $rCfg.REQ_SIZE" }
	  }
	}


  private val cosCells = sinCells.map {
	it.copy(phase = Phase.COS)
  }


  val complexCells = sinCells.zip(cosCells).map { ComplexCell(it) }
}


val pop2D = Population(alongY = rCfg.CELLS_ALONG_Y, conCircles = rCfg.CON_CIRCLES).apply {
  println("pop2d size:${complexCells.size}")
}
val popLouie = Population(
  alongY = false,
  conCircles = false,
  X0_ABS_MINMAX = 0.0,
  X0_STEP = 1.0,
  CELL_X0_STEP_MULT = 1,
  THETA_MIN = 0.0,
  THETA_MAX = 90.0,
  CELL_THETA_STEP = 100.0,
  REQ_SIZE = 1
).apply {
  println("popLouie size:${complexCells.size}")
}