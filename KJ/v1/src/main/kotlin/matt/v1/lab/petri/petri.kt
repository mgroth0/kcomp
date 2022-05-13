package matt.v1.lab.petri

import matt.kjlib.commons.USER_HOME
import matt.kjlib.file.get
import matt.kjlib.jmath.div
import matt.kjlib.jmath.sqrt
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.log.err
import matt.kjlib.ranges.step
import matt.kjlib.stream.forEachPairing
import matt.klibexport.klibexport.allUnique
import matt.v1.compcache.DivNorm
import matt.v1.compcache.Point
import matt.v1.compcache.Polar
import matt.v1.compcache.Radians
import matt.v1.mat.MatMat
import matt.v1.mat.saveMatFile
import matt.v1.model.AZERO
import matt.v1.model.Cell
import matt.v1.model.Circle
import matt.v1.model.ComplexCell
import matt.v1.model.Field
import matt.v1.model.FieldLocAndOrientation
import matt.v1.model.Phase
import matt.v1.model.Phase.SIN
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import org.apfloat.Apfloat
import kotlin.math.pow

val ROSENBERG_TD_C = 1.0*(10.0.pow(-4))

data class PopulationConfig(

  val cellX0AbsMinmax: Double = 15.0,
  val cellX0Step: Double = 0.2,
  val sampleStep: Double = 0.2, /*"much smaller"*/
  val cellX0StepMult: Double = 1.0,
  val prefThetaMin: Apfloat = Apfloat(0.0),
  val prefThetaMax: Apfloat = Apfloat(179.0),
  val popCCSpacingThetaStep: Double = 90.0,
  val reqSize: Int? = null, /*27_180=rosenberg*/ /*sanity*/
  val cellPrefThetaStep: Double = 1.0,
  val sampleCCThetaStep: Apfloat = Apfloat(90.0),
  val matCircles: Boolean = false,
  val alongY: Boolean = false,
  val conCircles: Boolean = false,
  val proportionalCellSigmas: Boolean = false,
  val cellSFmin: Apfloat = 4.0.toApfloat(),
  val cellSFmax: Apfloat = 4.0.toApfloat(),
  val cellSFstep: Apfloat = 1.0.toApfloat(),
  val stimSF: Double = 5.75,/*/ (2 * PI),*/
  val stimSigma: Double = 1.55,
  val baseContrast: Double = 1.0, /*technically part of stim not pop*/
  val DC_BASELINE_ACTIVITY: Double = 2.0,
  val baseDNGain: Double = ROSENBERG_TD_C,
  val semiSaturationConstant: Double = 1.0,
  val baseSigmaPooling: Apfloat = sqrt(5.0.toApfloat()),
  val baseSx: Apfloat = 0.7.toApfloat(),
  val baseSy: Apfloat = 1.2.toApfloat(),
  val fieldAbsMinMax: Double = /*5.0 */22.5  /*"make sure its zero-ed on all corners for edge neurons"*/
) {

  val fieldHW = fieldAbsMinMax*2/sampleStep

  val baseDivNorm by lazy {
	DivNorm( /*tdDivNorm*/
	  D = null,
	  c = baseDNGain.toApfloat(), /*rosenberg*/
	  v = semiSaturationConstant.toApfloat(),
	  S = null
	)
  }

  private val baseField by lazy {
	FieldLocAndOrientation(
	  tDegrees = 0.0,
	  X0 = -cellX0AbsMinmax,
	  Y0 = AZERO.toDouble(),
	  field = Field(
		absLimXY = fieldAbsMinMax,
		sampleStep = sampleStep
	  )
	)
  }


  val baseStim by lazy {
	Stimulus(
	  popCfg = this,
	  f = baseField,
	  a = baseContrast,
	  s = stimSigma,
	  SF = stimSF,
	)
  }


  val baseSimpleSinCell by lazy {
	SimpleCell(
	  popCfg = this,
	  f = baseField,


	  sx = (if (proportionalCellSigmas) (baseSx*cellSFmin)/cellSFmin/*thisCellSF*/ else baseSx).toDouble(),
	  sy = (if (proportionalCellSigmas) (baseSy*cellSFmin)/cellSFmin/*thisCellSF*/ else baseSy).toDouble(),
	  /*Rosenberg used an elliptical receptive field without explanation. This may interfere with some orientation-based results*/
	  /*sx = 0.95,
	  sy = 0.95,*/

	  SF = cellSFmin.toDouble(),
	  phase = SIN
	)
  }

  fun perfectStimFor(cell: Cell) = baseStim.copy(
	f = baseStim.f.copy(
	  tDegrees = cell.tDegrees,
	  X0 = cell.X0,
	  Y0 = cell.Y0
	),
	SF = cell.SF
  )
}


class Population(
  val cfg: PopulationConfig,
) {
  init {
	println("building pop")
	require(!(cfg.alongY && cfg.conCircles))
  }


  val centralCell: ComplexCell by lazy {
	complexCells
	  .filter { it.tDegrees == cfg.prefThetaMin.toDouble() }
	  .filter { it.SF == cfg.cellSFmin.toDouble() }
	  .filter {
		it.X0 >= 0.0 && it.Y0 >= 0.0
	  }
	  .minByOrNull { it.X0 + it.Y0 }!!
  }

  val sampleMatSize get() = intArrayOf(centralCell.sinCell.mat[0].size, centralCell.sinCell.mat.size)

  fun saveDebugMat(stim: Stimulus, r: Double) {
	mapOf(
	  "sinRF" to MatMat(centralCell.sinCell.mat),
	  "cosRF" to MatMat(centralCell.cosCell.mat),
	  "stim" to MatMat(stim.mat),
	  "R" to r,
	).saveMatFile(USER_HOME["desktop"]["temp.mat"])
  }

  fun centralCellWithSF(sf: Apfloat) = complexCells
	.filter { it.tDegrees == cfg.prefThetaMin.toDouble() }
	.filter { it.SF == sf.toDouble() }
	.filter {
	  it.X0 >= 0.0 && it.Y0 >= 0.0
	}
	.minByOrNull { it.X0 + it.Y0 }!!


  /*must ue apfloat here to get exact values and avoid double artifacts and match matlab!*/
  private val cellSpacialRange =
	(Apfloat(-cfg.cellX0AbsMinmax)..Apfloat(cfg.cellX0AbsMinmax) step (Apfloat(cfg.cellX0Step)*Apfloat(
	  cfg.cellX0StepMult
	)))


  private val circle by lazy { Circle(radius = cfg.cellX0AbsMinmax) }
  private val sinCells by lazy {
	cellSpacialRange
	  .flatMapIndexed { circI, x0F ->
		val x0 = x0F.toDouble()
		/*must use apfloat here for accurate range!*/
		(cfg.prefThetaMin..cfg.prefThetaMax step (cfg.cellPrefThetaStep.toApfloat())).flatMap { t ->
		  /*println("t=${t}")*/
		  /*must use apfloat here for accurate range!*/
		  (cfg.cellSFmin..cfg.cellSFmax step (cfg.cellSFstep)).flatMap { sf ->
			/*println("sf1=${sf}")*/
			if (cfg.conCircles) {
			  when {
				x0 < 0.0  -> listOf()
				x0 == 0.0 -> mutableListOf(
				  cfg.baseSimpleSinCell.copy(
					f = cfg.baseSimpleSinCell.f.copy(
					  X0 = 0.0,
					  Y0 = 0.0,
					  tDegrees = t.toDouble(),
					),
					SF = sf.toDouble(),
					sx = (if (cfg.proportionalCellSigmas) (cfg.baseSx*cfg.cellSFmin)/sf else cfg.baseSx).toDouble(),
					sy = (if (cfg.proportionalCellSigmas) (cfg.baseSy*cfg.cellSFmin)/sf else cfg.baseSy).toDouble(),
				  )
				)
				else      -> {
				  val cells =
					mutableListOf<SimpleCell>()
				  val circleThetaStep = (cfg.popCCSpacingThetaStep)/*90*//(circI + 1)
				  var a = 0.0
				  while (a <= 360.0 - circleThetaStep) {
					val rads = Radians(a)
					/*println("x0=${x0},rads=${rads()}")*/
					val p = Polar(x0, rads)()
					/*println("a1=${a},p.x=${p.x},p.y=${p.y}")*/
					val theNew = cfg.baseSimpleSinCell.copy(
					  f = cfg.baseSimpleSinCell.f.copy(
						X0 = p.x,
						Y0 = p.y,
						tDegrees = t.toDouble()
					  ),
					  SF = sf.toDouble(),
					  sx = (if (cfg.proportionalCellSigmas) (cfg.baseSx*cfg.cellSFmin)/sf else cfg.baseSx).toDouble(),
					  sy = (if (cfg.proportionalCellSigmas) (cfg.baseSy*cfg.cellSFmin)/sf else cfg.baseSy).toDouble(),
					)
					if (theNew in cells) {
					  err(
						"gotcha 1\nt=${t}\nsf=${sf}\na=${a}\ncells.size=${cells.size}\ncircleThetaStep=${circleThetaStep}"
					  )
					}
					cells += theNew
					a += circleThetaStep
				  }
				  cells
				}
			  }
			} else {
			  (if (cfg.alongY) cellSpacialRange else listOf(0.0.toApfloat())).flatMap { y0 ->
				listOf(
				  cfg.baseSimpleSinCell.copy(
					f = cfg.baseSimpleSinCell.f.copy(X0 = x0.toDouble(), Y0 = y0.toDouble(), tDegrees = t.toDouble()),
					SF = sf.toDouble(),
					sx = (if (cfg.proportionalCellSigmas) (cfg.baseSx*cfg.cellSFmin)/sf else cfg.baseSx).toDouble(),
					sy = (if (cfg.proportionalCellSigmas) (cfg.baseSy*cfg.cellSFmin)/sf else cfg.baseSy).toDouble(),
				  )
				)
			  }
			}
		  }
		}


	  }.apply {
	  }.filter {
		cfg.conCircles ||
			Point(x = it.X0, y = it.Y0).normDist(circle.center) <= circle.radius
	  }

	  .also {
		require(it.allUnique()) {
		  it.forEachPairing {
			if (first == second) {
			  println("$first same as $second")
			}
		  }
		  "stupid"
		}
		if (cfg.reqSize != null) {
		  require(it.size == cfg.reqSize) { "size is ${it.size} but should be ${cfg.reqSize}" }
		}
	  }
  }

  private val cosCells by lazy {
	sinCells.map {
	  it.copy(phase = Phase.COS)
	}
  }


  val complexCells: List<ComplexCell> by lazy { sinCells.zip(cosCells).map { ComplexCell(it) } }



}

val rosenbergPop = PopulationConfig(
  cellX0AbsMinmax = 15.0,
  cellX0Step = 0.2,
  cellX0StepMult = 1.0,
  prefThetaMin = 0.0.toApfloat(),
  prefThetaMax = 179.0.toApfloat(),
  cellPrefThetaStep = 1.0,
  /*reqSize = 27_180,*/
  matCircles = false,
  alongY = false,
  conCircles = false,
  proportionalCellSigmas = false,
  baseContrast = 1.0,

  /*cellSFmin = (1.0 / 4.0).toApfloat(),
  cellSFmax = (1.0 / 4.0).toApfloat(),*/

  cellSFmin = (4.0/*/(2.0*PI)*/).toApfloat(),
  cellSFmax = (4.0/*/(2.0*PI)*/).toApfloat(),

  cellSFstep = 100.0.toApfloat(),
)

val pop2D = PopulationConfig(
  conCircles = true,
  cellX0AbsMinmax = 3.0,
  cellX0Step = 1.0,
  cellPrefThetaStep = 30.0,
  /*stimRecCCThetaStep = 30.0,*/
)
val popLouie = pop2D.copy(
  cellX0AbsMinmax = 0.0,
  prefThetaMax = 90.0.toApfloat(),
  popCCSpacingThetaStep = 500.0,
  cellPrefThetaStep = 500.0,
  reqSize = 1
)
val popLouieMoreCells = popLouie.copy(
  conCircles = true,
  cellX0AbsMinmax = 1.0,
  prefThetaMax = 10.0.toApfloat(),
  popCCSpacingThetaStep = 90.0,
  reqSize = 5
)
val popLouieFullThetaCells = popLouieMoreCells.copy(
  prefThetaMax = 179.0.toApfloat(),
  cellPrefThetaStep = 1.0,
  reqSize = 180*5
)