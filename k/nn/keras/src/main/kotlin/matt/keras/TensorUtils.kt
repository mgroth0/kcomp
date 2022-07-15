package matt.keras

import org.tensorflow.ndarray.StdArrays
import org.tensorflow.types.TFloat32
import org.tensorflow.types.TInt64

object Tensors {

  @JvmStatic fun create(data: LongArray): TInt64 = TInt64.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun create(data: Array<LongArray>): TInt64 = TInt64.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun create(data: Array<Array<LongArray>>): TInt64 = TInt64.tensorOf(StdArrays.ndCopyOf(data))

  @JvmStatic fun create(data: FloatArray): TFloat32 = TFloat32.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun create(data: Array<FloatArray>): TFloat32 = TFloat32.tensorOf(StdArrays.ndCopyOf(data))
  @JvmStatic fun create(data: Array<Array<FloatArray>>): TFloat32 = TFloat32.tensorOf(StdArrays.ndCopyOf(data))

}