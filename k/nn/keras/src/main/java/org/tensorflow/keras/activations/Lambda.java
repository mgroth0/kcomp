package org.tensorflow.keras.activations;

import org.tensorflow.Operand;
import org.tensorflow.keras.mixin.ActivationFunction;
import org.tensorflow.op.Ops;
import org.tensorflow.types.family.TType;

/**
 * Creates an `Activation` from an unnamed function.
 * @param <T>
 */
public class Lambda<T extends TType> extends Activation<T> {
    private ActivationFunction<T> activation;

    /**
     * Creates an Activation function.
     * @param unnamedActivation An activation function.
     */
    public Lambda(ActivationFunction<T> unnamedActivation) {
        super();
        this.activation = unnamedActivation;
    }

    /**
     * Applies the given activation.
     */
    @Override
    protected Operand<T> call(Ops tf, Operand<T> inputs) {
        return activation.apply(tf, inputs);
    }
}
