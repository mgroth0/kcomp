package matt.v1.jsonpoint

import kotlinx.serialization.Serializable
import matt.kjlib.jmath.point.APoint
import matt.klib.lang.NEVER
import matt.klib.math.BasicPoint
import matt.klib.math.Point
import matt.reflect.NoArgConstructor


@NoArgConstructor
@Serializable
class JsonPoint(
  override val x: Double, override val y: Double
): /*SimpleJson<JsonPoint>(typekey = null),*/ Point {
//  override val x by JsonDoubleProp(x)
//  override val y by JsonDoubleProp(y)
  override val xDouble get() = x
  override val yDouble get() = y
  override fun clone(newX: Number?, newY: Number?): Point {
	return JsonPoint(x = newX?.toDouble() ?: x, y = newY?.toDouble() ?: y)
  }

  override fun toBasicPoint(): BasicPoint {
	return BasicPoint(x = x, y = y)
  }
}


fun List<Point>.toJsonPoints() = map { it.toJsonPoint() }

fun Point.toJsonPoint() = when (this) {
  is JsonPoint  -> this
  is BasicPoint -> JsonPoint(x = x, y = y)
  is APoint     -> JsonPoint(x = xDouble, y = yDouble)
  else          -> NEVER
}

