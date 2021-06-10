package matt.v1.lab.rcfg

import matt.kjlib.jmath.rem
import matt.kjlib.jmath.times
import matt.kjlib.jmath.toApfloat
import matt.kjlib.jmath.toApint
import org.apfloat.Apcomplex

private val BASE_SIGMA_STEP = 0.1
private val FASTER_SIGMA_STEP = 0.25

enum class ResourceUsageCfg(
  val X0_ABS_MINMAX: Double = 15.0,
  val X0_DIST_MAX: Double = 10.0,
  val X0_STEP: Double = 0.2,
  val CELL_THETA_STEP: Double = 1.0,
  val CELL_X0_STEP_MULT: Int = 1,
  val F3B_STEP: Double = 1.0,
  val F3C_STEP: Double = 0.005,
  val F3D_STEP: Double = BASE_SIGMA_STEP,
  val F5C_STEP: Double = 0.2,
  val F5D_STEP: Double = 10.0,
  val FS1_STEP: Double = 0.25,
  val CELLS_ALONG_Y: Boolean = false,
  val CON_CIRCLES: Boolean = false,
  val MAT_CIRCLES: Boolean = false
) {
  FINAL(X0_STEP = 0.2),


  DEV(
	CELL_THETA_STEP = 10.0,
	CELL_X0_STEP_MULT = 5,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = FINAL.F5C_STEP*2,
	FS1_STEP = 10.0,
  ),

  CIRCLE(
	CON_CIRCLES = true,
	X0_ABS_MINMAX = 3.0,
	X0_DIST_MAX = 2.0,
	X0_STEP = 0.5,
	CELL_X0_STEP_MULT = 2,
	CELL_THETA_STEP = 45.0,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	FS1_STEP = 15.0,
  ),

  @Suppress("unused")
  DEBUG(
	CELL_THETA_STEP = 30.0,
	CELL_X0_STEP_MULT = 40,
	F3B_STEP = 20.0,
	F3C_STEP = 10.0,
	F3D_STEP = FASTER_SIGMA_STEP,
	F5C_STEP = 3.0,
	F5D_STEP = 25.0
  );

  val FIELD_SIZE_MULT = 1/*complete guess*/

  val FIELD_ABS_MINMAX = X0_ABS_MINMAX*FIELD_SIZE_MULT


  val THETA_MIN = 0.0
  val THETA_MAX = 179.0

  val REQ_SIZE = 27_180

}


val rCfg = ResourceUsageCfg.CIRCLE.apply {
  Double
  require(X0_ABS_MINMAX.toApfloat()%(X0_STEP.toApfloat()*CELL_X0_STEP_MULT.toApint()) == Apcomplex.ZERO) {
	"""first mod got $X0_ABS_MINMAX % ($X0_STEP * $CELL_X0_STEP_MULT = ${X0_STEP*CELL_X0_STEP_MULT}) = ${X0_ABS_MINMAX%(X0_STEP*CELL_X0_STEP_MULT)}
	""".trimMargin()
  }
}