package matt.v1.mat

import matt.klib.lang.err
import us.hebi.matlab.mat.format.Mat5
import us.hebi.matlab.mat.types.Matrix
import java.io.File

fun Map<String, Any>.saveMatFile(f: File) {
  require(!f.exists())
  f.mkdirs()
  val matFile = Mat5.newMatFile().apply {
	this@saveMatFile.forEach { (s, m) ->
	  when (m) {
		is MatData -> addArray(s, m.getMatrix())
		is Matrix  -> addArray(s, m)
		is Double  -> addArray(s, MatScalar(m).getMatrix())
		else       -> err("saveMatFile cannot yet handle ${m::class.simpleName}")
	  }
	}
  }
  Mat5.writeToFile(matFile, f)
}

sealed class MatData {
  abstract fun getMatrix(): Matrix
}

class MatMat(val rows: Array<FloatArray>): MatData() {
  override fun getMatrix(): Matrix {
	return Mat5.newMatrix(intArrayOf(rows[0].size, rows.size)).apply {
	  rows.forEachIndexed { rIndex, row ->
		row.forEachIndexed { cIndex, v ->
		  this.setDouble(
			cIndex, rIndex, v.toDouble()
		  ) /*trust me, this is the order of col,row that matches rosenberg's code. dont know if its generally the right way but this is something i find arbitrary*/
		}
	  }
	}
  }

}

class MatScalar(val value: Double): MatData() {
  override fun getMatrix(): Matrix {
	return Mat5.newScalar(value)
  }
}