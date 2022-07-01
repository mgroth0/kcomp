package matt.v1.model.vis

import matt.stream.flatten
import matt.stream.forEachNested
import matt.v1.model.FieldGenerator
import matt.v1.model.vis.NormMethod.ADD_POINT_5
import matt.v1.model.vis.NormMethod.RATIO
import kotlin.system.exitProcess

/*just for visualizing*/
enum class NormMethod {
  RATIO, ADD_POINT_5
}

val NORM = ADD_POINT_5

fun FieldGenerator.calcVis(): Array<DoubleArray> {
  val v = (0 until fieldCfg.length).map { DoubleArray(fieldCfg.length) }.toTypedArray()
  when (NORM) {
	RATIO       -> {
	  val matMin by lazy {
		mat.flatten().minOf { it.toDouble() }
	  }
	  val matMax by lazy { mat.flatten().maxOf { it.toDouble() } }
	  if (matMin == matMax) {
		println("stopping because min equals max")
		exitProcess(0)
	  }
	  val diff = matMax - matMin
	  (0 until fieldCfg.length).forEachNested { x, y ->
		if (!mat[x][y].isNaN()) {
		  v[x][y] = (mat[x][y] - matMin)/diff
		} else {
		  v[x][y] = Double.NaN
		}

	  }
	}

	ADD_POINT_5 -> {
	  (0 until fieldCfg.length).forEachNested { x, y ->        /*the fact that the number is sometimes more than 1.0 / less than  0.0 is concerning*/        /*it seems to start at a=0.51*/        /*maybe in the model it doesn't matter since mapping to pixel values is just for visualization*/

		if (!mat[x][y].isNaN()) {
		  v[x][y] = /*max(min(*/mat[x][y].toDouble()/* + 0.5, 1.0), 0.0)*/
		} else {
		  v[x][y] = Double.NaN
		}

	  }
	}
  }
  return v
}