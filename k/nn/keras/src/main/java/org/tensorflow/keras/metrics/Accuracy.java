package org.tensorflow.keras.metrics;

import org.tensorflow.Operand;
import org.tensorflow.op.core.Shape;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.family.TNumber;

public class Accuracy extends Metric {
  @Override
  public <T extends TNumber> Operand<T> apply(Ops tf, Class<T> dtype, Operand<T> output, Operand<T> label) {
    Operand<TInt64> predicted = tf.argMax(output, tf.constant(1));
    Operand<TInt64> expected = tf.argMax(label, tf.constant(1));

    return tf.mean(tf.cast(tf.equal(predicted, expected), dtype), tf.constant(0));
  }
}
