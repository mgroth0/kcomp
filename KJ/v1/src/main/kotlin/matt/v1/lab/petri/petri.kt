package matt.v1.lab.petri

//import matt.kjlib.commons.USER_HOME
import matt.kbuild.USER_HOME
import matt.kjlib.file.get
import matt.kjlib.jmath.div
import matt.kjlib.jmath.point.BasicPoint
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.lang.err
import matt.kjlib.ranges.step
import matt.kjlib.stream.forEachPairing
import matt.klibexport.klibexport.allUnique
import matt.v1.lab.petri.PopShape.CON_CIRCLES
import matt.v1.lab.petri.PopShape.HLINE
import matt.v1.lab.petri.PopShape.MAT_CIRCLE
import matt.v1.lab.petri.PopShape.SQUARE
import matt.v1.low.Circle
import matt.v1.low.PhaseType.COS
import matt.v1.low.PhaseType.SIN
import matt.v1.low.Polar
import matt.v1.low.Radians
import matt.v1.mat.MatMat
import matt.v1.mat.saveMatFile
import matt.v1.model.Orientation
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import matt.v1.model.combined.CombinedConfig
import matt.v1.model.complexcell.ComplexCell
import org.apfloat.Apfloat


enum class PopShape {
  HLINE,
  SQUARE,
  MAT_CIRCLE,
  CON_CIRCLES
}

data class PopulationConfig(
  val cellX0AbsMinmax: Double = 15.0,
  val cellX0Step: Double = 0.2,
  val cellX0StepMult: Double = 1.0,
  val popShape: PopShape = HLINE,
  val prefThetaMin: Apfloat = Apfloat(0.0),
  val prefThetaMax: Apfloat = Apfloat(179.0),
  val popCCSpacingThetaStep: Double = 90.0,
  val reqSize: Int? = null, /*27_180=rosenberg*/ /*sanity*/
  val cellPrefThetaStep: Double = 1.0,
  val proportionalCellSigmas: Boolean = false,
  val cellSFmin: Apfloat = 4.0.toApfloat(),
  val cellSFmax: Apfloat = 4.0.toApfloat(),
  val cellSFstep: Apfloat = 1.0.toApfloat(),
  val baseSx: Apfloat = 0.7.toApfloat(),
  val baseSy: Apfloat = 1.2.toApfloat(),
)


class Population(
  val cfg: CombinedConfig,
) {
  init {
	println("building pop")
  }


  val centralCell: ComplexCell by lazy {
	complexCells
	  .filter { it.o.tDegrees == cfg.populationConfig.prefThetaMin.toDouble() }
	  .filter { it.SF == cfg.populationConfig.cellSFmin.toDouble() }
	  .filter {
		it.xDouble >= 0.0 && it.yDouble >= 0.0
	  }
	  .minByOrNull { it.xDouble + it.yDouble }!!
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
	.filter { it.o.tDegrees == cfg.populationConfig.prefThetaMin.toDouble() }
	.filter { it.SF == sf.toDouble() }
	.filter {
	  it.xDouble >= 0.0 && it.yDouble >= 0.0
	}
	.minByOrNull { it.xDouble + it.yDouble }!!


  /*must ue apfloat here to get exact values and avoid double artifacts and match matlab!*/
  private val cellSpacialRange =
	(Apfloat(-cfg.populationConfig.cellX0AbsMinmax)..Apfloat(cfg.populationConfig.cellX0AbsMinmax) step (Apfloat(
	  cfg.populationConfig.cellX0Step
	)*Apfloat(
	  cfg.populationConfig.cellX0StepMult
	)))


  private val circle by lazy { Circle(radius = cfg.populationConfig.cellX0AbsMinmax) }


  private val sinCells: Iterable<SimpleCell<SIN>> by lazy {
	cellSpacialRange
	  .flatMapIndexed { circI, x0F ->
		val x0 = x0F.toDouble()
		/*must use apfloat here for accurate range!*/
		(cfg.populationConfig.prefThetaMin..cfg.populationConfig.prefThetaMax step (cfg.populationConfig.cellPrefThetaStep.toApfloat())).flatMap { t ->
		  /*println("t=${t}")*/
		  /*must use apfloat here for accurate range!*/
		  (cfg.populationConfig.cellSFmin..cfg.populationConfig.cellSFmax step (cfg.populationConfig.cellSFstep)).flatMap { sf ->
			/*println("sf1=${sf}")*/
			if (cfg.populationConfig.popShape == CON_CIRCLES) {
			  when {
				x0 < 0.0  -> listOf()
				x0 == 0.0 -> mutableListOf(
				  cfg.baseSimpleSinCell.copy(
					fieldLoc = BasicPoint(0, 0),
					SF = sf.toDouble(),
					sx = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSx*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSx).toDouble(),
					sy = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSy*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSy).toDouble(),
				  )
				)
				else      -> {
				  val cells =
					mutableListOf<SimpleCell<SIN>>()
				  val circleThetaStep = (cfg.populationConfig.popCCSpacingThetaStep)/*90*//(circI + 1)
				  var a = 0.0
				  while (a <= 360.0 - circleThetaStep) {
					val rads = Radians(a)
					/*println("x0=${x0},rads=${rads()}")*/
					val p = Polar(x0, rads)()
					/*println("a1=${a},p.x=${p.x},p.y=${p.y}")*/
					val theNew = cfg.baseSimpleSinCell.copy(
					  fieldLoc = p,
					  //					  f = cfg.baseSimpleSinCell.f.copy(
					  //						X0 = p.x,
					  //						Y0 = p.y,
					  //						tDegrees = t.toDouble()
					  //					  ),
					  SF = sf.toDouble(),
					  sx = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSx*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSx).toDouble(),
					  sy = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSy*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSy).toDouble(),
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
			  (if (cfg.populationConfig.popShape == SQUARE) cellSpacialRange else listOf(
				0.0.toApfloat()
			  )).flatMap { y0 ->
				listOf(
				  cfg.baseSimpleSinCell.copy(
					//					f = cfg.baseSimpleSinCell.f.copy(X0 = x0.toDouble(), Y0 = y0.toDouble(), tDegrees = t.toDouble()),
					fieldLoc = BasicPoint(x0, y0.toDouble()),
					o = Orientation(t.toDouble()),
					SF = sf.toDouble(),
					sx = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSx*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSx).toDouble(),
					sy = (if (cfg.populationConfig.proportionalCellSigmas) (cfg.populationConfig.baseSy*cfg.populationConfig.cellSFmin)/sf else cfg.populationConfig.baseSy).toDouble(),
				  )
				)
			  }
			}
		  }
		}


	  }.apply {
	  }.filter {
		cfg.populationConfig.popShape != MAT_CIRCLE || BasicPoint(x = it.xDouble, y = it.yDouble).normDist(
		  circle.center
		) <= circle.radius
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
		if (cfg.populationConfig.reqSize != null) {
		  require(
			it.size == cfg.populationConfig.reqSize
		  ) { "size is ${it.size} but should be ${cfg.populationConfig.reqSize}" }
		}
	  }
  }

  private val cosCells: List<SimpleCell<COS>> by lazy {
	sinCells.map { it.toCellWithPhase(COS) }
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
  proportionalCellSigmas = false,
  /*cellSFmin = (1.0 / 4.0).toApfloat(),
  cellSFmax = (1.0 / 4.0).toApfloat(),*/

  cellSFmin = (4.0/*/(2.0*PI)*/).toApfloat(),
  cellSFmax = (4.0/*/(2.0*PI)*/).toApfloat(),

  cellSFstep = 100.0.toApfloat(),
)
