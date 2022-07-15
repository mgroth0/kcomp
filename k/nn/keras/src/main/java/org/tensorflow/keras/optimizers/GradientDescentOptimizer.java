package org.tensorflow.keras.optimizers;

import kotlin.NotImplementedError;
import org.tensorflow.Operand;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.Gradients;
import org.tensorflow.op.core.Variable;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.family.TNumber;

import java.util.ArrayList;
import java.util.List;

public class GradientDescentOptimizer<T extends TNumber> extends Optimizer<T> {
    private final TFloat32 lr;
    private Constant<T> alpha;

    public GradientDescentOptimizer(TFloat32 lr) {
        this.lr = lr;
    }

    @Override
    public void build(Ops tf) {
        throw new NotImplementedError("""
        this.alpha = tf.constant(lr, getDtype());        
                """);

    }

    @Override
    public List<Operand<T>> applyGradients(Ops tf, List<Variable<T>> weights, Gradients gradients) {
        List<Operand<T>> targets = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            targets.add(tf.applyGradientDescent(weights.get(i), alpha, gradients.dy(i)));
        }

        return targets;
    }
}


