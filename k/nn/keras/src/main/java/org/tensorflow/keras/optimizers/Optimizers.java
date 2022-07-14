package org.tensorflow.keras.optimizers;

public enum Optimizers {
  sgd;

  public static <T extends Number> Optimizer<T> select(Optimizers optimizerType) {
    switch (optimizerType) {
      case sgd:
        return new GradientDescentOptimizer<>(0.2f);
      default:
        throw new IllegalArgumentException("Invalid Optimizer Type.");
    }
  }
}
