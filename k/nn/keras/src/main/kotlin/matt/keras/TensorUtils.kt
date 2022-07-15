package matt.keras

import org.tensorflow.ndarray.StdArrays
import org.tensorflow.types.TFloat32

object Tensors {
  @JvmStatic fun create(data: Array<FloatArray>): TFloat32 = TFloat32.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun create(data: Array<Array<FloatArray>>): TFloat32 = TFloat32.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun test(data: Float) = data + 1
}