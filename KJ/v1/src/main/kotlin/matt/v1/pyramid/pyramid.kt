package matt.v1.pyramid

import matt.kjlib.log.NEVER
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import kotlin.math.roundToInt

const val PYRAMID_ALPHA = 0.4
const val PYRAMID_W1 = 1.0/4.0
const val PYRAMID_W2 = 1.0/4.0 - PYRAMID_ALPHA/2.0
val UP_MULT = (1.0/PYRAMID_W1).roundToInt()


fun NDArray<Int, D3>.pyrDown(): NDArray<Int, D3> {
  val r = mk.empty<Int, D3>((this.shape[0] + 1)/2, (this.shape[1] + 1)/2, 3)
  (0..2).forEach { c ->
	r.multiIndices.iterator().forEach { indices ->
	  val x = indices[0]
	  val y = indices[1]
	  val weights = mutableListOf<Double>()
	  val pixels = mutableListOf<Int>()
	  val g = (2*x - 2..2*x + 2).flatMap { xx ->
		val w0 = when (xx) {
		  2*x - 2 -> PYRAMID_W2
		  2*x - 1 -> PYRAMID_W1
		  2*x     -> PYRAMID_ALPHA
		  2*x + 1 -> PYRAMID_W1
		  2*x + 2 -> PYRAMID_W2
		  else    -> NEVER
		}
		(2*y - 2..2*y + 2).mapNotNull { yy ->
		  val w1 = when (yy) {
			2*y - 2 -> PYRAMID_W2
			2*y - 1 -> PYRAMID_W1
			2*y     -> PYRAMID_ALPHA
			2*y + 1 -> PYRAMID_W1
			2*y + 2 -> PYRAMID_W2
			else    -> NEVER
		  }
		  val w = w0*w1
		  if (xx < 0 || yy < 0 || xx >= this.shape[0] || yy >= this.shape[1]) {
			null.also { weights += w }
		  } else {
			w*(this[xx, yy, c].also { pixels += it })
		  }
		}
	  }
	  val avg = pixels.sum()/pixels.size
	  val sum = g.sum() + weights.sumOf { it*avg }
	  r[x, y, c] = (sum).roundToInt()
	}
  }
  return r
}

fun NDArray<Int, D3>.pyrUp(): NDArray<Int, D3> {
  val r = mk.empty<Int, D3>((this.shape[0] - 1)*2, (this.shape[1] - 1)*2, 3)
  (0..2).forEach { c ->
	r.multiIndices.iterator().forEach { indices ->
	  val x = indices[0]
	  val y = indices[1]
	  val weights = mutableListOf<Double>()
	  val pixels = mutableListOf<Int>()
	  val g = ((x - 2)/2..(x + 2)/2).flatMap { xx ->
		val w0 = when (xx) {
		  (x - 2)/2 -> PYRAMID_W2
		  x/2       -> PYRAMID_ALPHA
		  (x + 2)/2 -> PYRAMID_W2
		  else      -> NEVER
		}
		((y - 2)/2..(y + 2)/2).mapNotNull { yy ->
		  val w1 = when (yy) {
			(y - 2)/2 -> PYRAMID_W2
			y/2       -> PYRAMID_ALPHA
			(y + 2)/2 -> PYRAMID_W2
			else      -> NEVER
		  }
		  val w = w0*w1
		  if (xx < 0 || yy < 0 || xx >= this.shape[0] || yy >= this.shape[1]) {
			null.also { weights += w }
		  } else {
			w*(this[xx, yy, c].also { pixels += it })
		  }
		}
	  }
	  val avg = pixels.sum()/pixels.size
	  val sum = g.sum() + weights.sumOf { it*avg }
	  r[x, y, c] = (sum*UP_MULT).roundToInt()
	}
  }
  return r
}

@JvmName("pyrUpIntD2")
fun NDArray<Int, D2>.pyrUp(): NDArray<Int, D2> {
  val r = mk.empty<Int, D2>((this.shape[0] - 1)*2, (this.shape[1] - 1)*2)
  r.multiIndices.iterator().forEach { indices ->
	val x = indices[0]
	val y = indices[1]
	val weights = mutableListOf<Double>()
	val pixels = mutableListOf<Int>()
	val g = ((x - 2)/2..(x + 2)/2).flatMap { xx ->
	  val w0 = when (xx) {
		(x - 2)/2 -> PYRAMID_W2
		x/2       -> PYRAMID_ALPHA
		(x + 2)/2 -> PYRAMID_W2
		else      -> NEVER
	  }
	  ((y - 2)/2..(y + 2)/2).mapNotNull { yy ->
		val w1 = when (yy) {
		  (y - 2)/2 -> PYRAMID_W2
		  y/2       -> PYRAMID_ALPHA
		  (y + 2)/2 -> PYRAMID_W2
		  else      -> NEVER
		}
		val w = w0*w1
		if (xx < 0 || yy < 0 || xx >= this.shape[0] || yy >= this.shape[1]) {
		  null.also { weights += w }
		} else {
		  w*(this[xx, yy].also { pixels += it })
		}
	  }
	}
	val avg = pixels.sum()/pixels.size
	val sum = g.sum() + weights.sumOf { it*avg }
	r[x, y] = ((sum)*UP_MULT).roundToInt()
  }
  return r
}