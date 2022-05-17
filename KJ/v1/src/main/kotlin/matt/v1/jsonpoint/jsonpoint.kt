package matt.v1.jsonpoint

import matt.json.custom.SimpleJson
import matt.kjlib.jmath.point.APoint
import matt.kjlib.jmath.point.BasicPoint
import matt.kjlib.jmath.point.Point
import matt.kjlib.log.NEVER
import matt.reflect.NoArgConstructor


@NoArgConstructor
class JsonPoint(
  x: Double? = null, y: Double? = null
): SimpleJson<JsonPoint>(typekey = null), Point {
  override val x by JsonDoubleProp(x)
  override val y by JsonDoubleProp(y)
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

