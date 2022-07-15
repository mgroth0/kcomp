package org.tensorflow.keras.initializers;

import org.tensorflow.Operand;
import org.tensorflow.keras.utils.Keras;
import org.tensorflow.op.Ops;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.family.TType;

public class Constant extends Initializer {
    private Object val;

    public  Constant(Object val) {
        this.val = val;
    }

    @Override
    public <T extends TType> Operand<T> initialize(Ops tf, Operand<TInt32> shape, Class<T> dtype) {
        return tf.fill(shape, tf.constant(val, dtype));
    }
}
