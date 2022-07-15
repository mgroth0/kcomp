package org.tensorflow.keras.initializers;


import kotlin.NotImplementedError;
import org.tensorflow.Operand;
import org.tensorflow.op.Ops;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.family.TNumber;
import org.tensorflow.types.family.TType;

public class RandomNormal extends Initializer {
    private float mean;
    private float stdev;
    private float p1;
    private float p2;

    public RandomNormal(float mean, float stdev, float p1, float p2) {
        super();
        this.mean = mean;
        this.stdev = stdev;
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public <T extends TType> Operand<T> initialize(Ops tf, Operand<TInt32> shape, Class<T> dtype) {
        throw new NotImplementedError("""
                return tf.parameterizedTruncatedNormal(
                           shape,
                           tf.constant(this.mean, dtype),
                           tf.constant(this.stdev, dtype),
                           tf.constant(this.p1, dtype),
                           tf.constant(this.p2, dtype)
                   );     
                """);

    }
}
