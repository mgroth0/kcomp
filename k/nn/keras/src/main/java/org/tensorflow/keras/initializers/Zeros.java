package org.tensorflow.keras.initializers;

import org.tensorflow.Operand;
import org.tensorflow.keras.utils.Keras;
import org.tensorflow.op.Ops;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.family.TType;

public class Zeros extends Initializer {
  @Override
  public <T extends TType> Operand<T> initialize(Ops tf, Operand<TInt32> shape, Class<T> dtype) {
    return tf.zeros(shape, dtype);
  }
}
