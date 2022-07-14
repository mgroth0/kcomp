package org.tensorflow.keras.data;

import org.tensorflow.Operand;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;

public interface GraphLoader<T> extends Dataset<T> {

  Operand<T>[] getBatchOperands();

  void build(Ops tf);

  long size();

  Session.Runner  feedSessionRunner(Session.Runner runner, long batch);
}
