package matt.v1.model.field

import matt.kjlib.jmath.ranges.step
import matt.kjlib.jmath.unaryMinus
import matt.klib.math.BasicPoint
import matt.v1.low.Circle
import matt.v1.model.field.FieldShape.SQUARE
import org.apfloat.Apfloat

enum class FieldShape {
  SQUARE,
  MAT_CIRCLE,
  CON_CIRCLE
}

data class FieldConfig(
  val sampleStep: Double = 0.2, /*"much smaller"*/
  val sampleCCThetaStep: Apfloat = Apfloat(90.0),
  val shape: FieldShape = SQUARE,
  val fieldAbsMinMax: Double = /*5.0 */22.5  /*"make sure its zero-ed on all corners for edge neurons"*/
) {
  val fieldHW = fieldAbsMinMax*2/sampleStep

  /*need apfloat here to get same mat size as matlab*/
  val fullRange =
	(-Apfloat(fieldAbsMinMax)..Apfloat(fieldAbsMinMax) step Apfloat(sampleStep)).toList().map { it.toDouble() }
  val halfRange by lazy {
	(Apfloat(sampleStep)..Apfloat(fieldAbsMinMax) step Apfloat(
	  sampleStep
	)).map { it.toDouble() }
  }
  val length = fullRange.size
  val circle = Circle(radius = fieldAbsMinMax)
  val visCircle = Circle(radius = fieldAbsMinMax, BasicPoint(x = fieldAbsMinMax/2, y = fieldAbsMinMax/2))
}