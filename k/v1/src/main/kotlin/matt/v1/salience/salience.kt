package matt.v1.salience

import matt.async.date.withStopwatch
import matt.hurricanefx.intColorToFXColor
import matt.kjlib.jmath.convolve
import matt.klib.lang.go
import matt.klib.math.intMean
import matt.v1.pyramid.pyrDown
import matt.v1.pyramid.pyrUp
import matt.v1.salience.FeatureType.BLUE_YELLOW
import matt.v1.salience.FeatureType.INTENSITY
import matt.v1.salience.FeatureType.RED_GREEN
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.sum
import java.awt.image.BufferedImage
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class FeatureType {
  INTENSITY,
  RED_GREEN,
  BLUE_YELLOW
}

data class FeatureDef(
  val center: Int,
  val surround: Int,
  val type: FeatureType
)

data class Salience(
  val thing: Boolean = true
) {

  companion object {
	val LPF_KERNEL = mk.empty<Double, D2>(5, 5).apply {
	  (0..4).forEach { x ->
		(0..4).forEach { y ->
		  this[x, y] = when {
			x == 0 || y == 0 || x == 4 || y == 4 -> 1.0/16.0
			x == 1 || y == 1 || x == 3 || y == 3 -> 1.0/4.0
			else                                 -> 3.0/8.0
		  }
		}
	  }
	}
  }


  fun computeFeatures(im: BufferedImage): Map<FeatureDef, NDArray<Int, D2>> = withStopwatch("salience filter") { t ->
	val r = mk.empty<Int, D3>(im.height, im.width, 3)
	mk.empty<Int, D2>(im.height, im.width)
	(0 until im.height).forEach { y ->
	  (0 until im.width).forEach { x ->
		intColorToFXColor(im.getRGB(x, y)).go {
		  r[x, y, 0] = (it.red*255).roundToInt()
		  r[x, y, 1] = (it.green*255).roundToInt()
		  r[x, y, 2] = (it.blue*255).roundToInt()
		}
	  }
	}

	t.toc("filter1")

	val spatialScales = mutableListOf(r)
	repeat((1..8).count()) {
	  spatialScales += spatialScales.last().pyrDown()
	}
	t.toc("filter2")

	val features = mutableMapOf<FeatureDef, NDArray<Int, D2>>()

	(2..4).forEach { center ->
	  (center + 3..center + 4).forEach { surround ->
		val scaleDiff = surround - center
		val centerIm = spatialScales[center]
		val surroundIm = spatialScales[surround]
		var featIntense = mk.empty<Int, D2>(centerIm.shape[0], centerIm.shape[1])
		var featRg = mk.empty<Int, D2>(centerIm.shape[0], centerIm.shape[1])
		var featBy = mk.empty<Int, D2>(centerIm.shape[0], centerIm.shape[1])

		(0 until centerIm.shape[0]).forEach { pxRow ->
		  var pxRowSur = pxRow
		  repeat((0 until scaleDiff).count()) {
			pxRowSur = pxRowSur.floorDiv(2)
		  }
		  (0 until centerIm.shape[1]).forEach { pxCol ->
			var pxColSur = pxCol
			repeat((0 until scaleDiff).count()) {
			  pxColSur = pxColSur.floorDiv(2)
			}
			val centerIntense = centerIm[pxRow, pxCol].sum()/3
			val surroundIntense = surroundIm[pxRowSur, pxColSur].sum()/3
			featIntense[pxRow, pxCol] = (centerIntense - surroundIntense).absoluteValue
			val centerPx = centerIm[pxRow, pxCol]
			val cenRed = centerPx[0]
			val cenGreen = centerPx[1]
			val cenBlue = centerPx[2]
			val cenYellow = intArrayOf(cenRed, cenGreen).intMean()
			val surroundPx = surroundIm[pxRowSur, pxColSur]
			val surRed = surroundPx[0]
			val surGreen = surroundPx[1]
			val surBlue = surroundPx[2]
			val surYellow = intArrayOf(surRed, surGreen).intMean()

			val centerRG = (cenRed - centerIntense) - (cenGreen - centerIntense)
			val surroundRG = (surGreen - surroundIntense) - (surRed - surroundIntense)
			featRg[pxRow, pxCol] = (centerRG - surroundRG).absoluteValue

			val centerBY = (cenBlue - centerIntense) - (cenYellow - centerIntense)
			val surroundBY = (surYellow - surroundIntense) - (surBlue - surroundIntense)

			featBy[pxRow, pxCol] = (centerBY - surroundBY).absoluteValue
		  }
		}


		val g0 = featIntense.convolve(LPF_KERNEL)
		@Suppress("UNUSED_VARIABLE") val ln = featIntense - g0.map { it.roundToInt() }
		@Suppress("UNUSED_VARIABLE") val orients = listOf(0, 45, 90, 135)


		(1..center).forEach { _ ->
		  featIntense = featIntense.pyrUp()
		  featRg = featRg.pyrUp()
		  featBy = featBy.pyrUp()
		}
		features[FeatureDef(center = center, surround = surround - center, type = INTENSITY)] = featIntense
		features[FeatureDef(center = center, surround = surround - center, type = RED_GREEN)] = featRg
		features[FeatureDef(center = center, surround = surround - center, type = BLUE_YELLOW)] = featBy

		t.toc("filter3")
	  }
	}
	return@withStopwatch features
  }
}

