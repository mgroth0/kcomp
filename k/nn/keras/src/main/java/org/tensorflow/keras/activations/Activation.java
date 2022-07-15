package org.tensorflow.keras.activations;

import org.tensorflow.Operand;
import org.tensorflow.op.core.Shape;
import org.tensorflow.keras.layers.Layer;
import org.tensorflow.keras.mixin.ActivationFunction;
import org.tensorflow.op.Ops;
import org.tensorflow.types.family.TType;

/**
 * Base activation function class.
 */
@SuppressWarnings("unchecked")
public abstract class Activation<T extends TType> extends Layer<T> {

  public Activation() {
    super(1);
  }

  @Override
  public void build(Ops tf, Shape inputShape) {
    // Activations don't need state to be built and added to a graph. Does nothing.
  }

  @Override
  public Shape computeOutputShape(Shape inputShape) {
    // Activation functions should not change the shape of the input.
    return inputShape;
  }

  @Override
  public Operand<T> call(Ops tf, Operand<T>... inputs) {
    return call(tf, inputs[0]);
  }

  /**
   * Calls the activation function. Override this when defining an activation function.
   */
  protected abstract Operand<T> call(Ops tf, Operand<T> inputs);
}
