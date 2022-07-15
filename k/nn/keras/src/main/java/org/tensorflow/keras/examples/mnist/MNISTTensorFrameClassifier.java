package org.tensorflow.keras.examples.mnist;

import org.tensorflow.*;
import org.tensorflow.keras.data.GraphLoader;
import org.tensorflow.keras.activations.Activations;
import org.tensorflow.keras.datasets.MNIST;
import org.tensorflow.keras.layers.Dense;
import org.tensorflow.keras.layers.Input;
import org.tensorflow.keras.losses.Loss;
import org.tensorflow.keras.losses.Losses;
import org.tensorflow.keras.metrics.Metric;
import org.tensorflow.keras.metrics.Metrics;
import org.tensorflow.keras.optimizers.GradientDescentOptimizer;
import org.tensorflow.keras.optimizers.Optimizer;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.op.Ops;
import org.tensorflow.types.TFloat32;
import org.tensorflow.utils.Pair;

import java.io.IOException;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked", "UnnecessaryLocalVariable"})
public class MNISTTensorFrameClassifier implements Runnable {
   private static final int INPUT_SIZE = 28 * 28;

   private static final float LEARNING_RATE = 0.15f;
   private static final int FEATURES = 10;
   private static final int BATCH_SIZE = 100;

   private static final int EPOCHS = 10;

   public static void main(String[] args) {
       MNISTTensorFrameClassifier mnist = new MNISTTensorFrameClassifier();
       mnist.run();

   }

   public void run() {
       try (Graph graph = new Graph()) {
           Ops tf = Ops.create(graph);

           // Load MNIST Dataset
           Pair<GraphLoader<TFloat32>, GraphLoader<TFloat32>> data;
           try {
               data = MNIST.graphLoaders();
           } catch (IOException e) {
               throw new IllegalArgumentException("Could not load MNIST dataset.");
           }

           try (GraphLoader<TFloat32> train = data.first();
                GraphLoader<TFloat32> test = data.second()) {

               Input inputLayer = new Input<>(/*Float.class,*/ INPUT_SIZE);
               Dense denseLayer = new Dense(FEATURES, Dense.Options.builder().setActivation(Activations.softmax).build());

               Loss loss = Losses.select(Losses.sparseCategoricalCrossentropy);
               Metric accuracy = Metrics.select(Metrics.accuracy);
               Optimizer<TFloat32> optimizer = new GradientDescentOptimizer(LEARNING_RATE);

               // Compile Model
               train.batch(BATCH_SIZE);
               train.build(tf);
               Operand<TFloat32>[] trainOps = train.getBatchOperands();

               test.batch(BATCH_SIZE);
               test.build(tf);
               Operand<TFloat32>[] testOps = test.getBatchOperands();

               inputLayer.build(tf, Shape.of(INPUT_SIZE));
               denseLayer.build(tf, inputLayer.computeOutputShape());
               optimizer.build(tf,TFloat32.class);


               // Fit Model (TRAIN)
               try (Session session = new Session(graph)) {
                   {
                       Session.Runner runner = session.runner();
                       Operand<TFloat32> XOp = trainOps[0];
                       Operand<TFloat32> yOp = trainOps[1];

                       // Compute Output / Loss / Accuracy
                       Operand<TFloat32> yTrue = yOp;
                       Operand<TFloat32> yPred = denseLayer.apply(tf, XOp);

                       Operand<TFloat32> batchLoss = loss.apply(tf,TFloat32.class, yTrue, yPred);
                       Operand<TFloat32> batchAccuracy = accuracy.apply(tf,TFloat32.class, yTrue, yPred);

                       List<Operand<TFloat32>> minimize = optimizer.minimize(tf, batchLoss, denseLayer.trainableWeights());


                       // Run initializer ops
                       for (Object op : denseLayer.initializerOps()) {
                           runner.addTarget((Operand<TFloat32>) op); //NOSONAR
                       }

                       runner.run();

                       for (int epoch = 0; epoch < EPOCHS; epoch++) {
                           float trainEpochAccuracy = 0;
                           float trainEpochLoss = 0;

                           // Load Batches
                           for (int i = 0; i < train.numBatches(); i++) {
                               runner = session.runner();
                               train.feedSessionRunner(runner, i);

                               for (Operand<TFloat32> op : minimize) {
                                   runner.addTarget(op);
                               }

                               runner.fetch(batchLoss);
                               runner.fetch(batchAccuracy);

                               List<Tensor> values = runner.run();
                               try (Tensor lossTensor = values.get(0);
                                    Tensor accuracyTensor = values.get(1)) {
                                   accuracyTensor.asRawTensor().data().asFloats();
                                   trainEpochAccuracy += accuracyTensor.asRawTensor().data().asFloats().getFloat(0) / train.numBatches();
                                   trainEpochLoss += lossTensor.asRawTensor().data().asFloats().getFloat(0) / train.numBatches();
                               }
                           }

                           System.out.println("Epoch " + epoch + " train accuracy: " + trainEpochAccuracy + "  loss: " + trainEpochLoss);
                       }
                   }

                   // Fit Model (TEST)
                   {
                       Session.Runner runner = session.runner();
                       Operand<TFloat32> XOp = testOps[0];
                       Operand<TFloat32> yOp = testOps[1];

                       // Compute Output / Loss / Accuracy
                       Operand<TFloat32> yTrue = yOp;
                       Operand<TFloat32> yPred = denseLayer.apply(tf, XOp);

                       Operand<TFloat32> batchLoss = loss.apply(tf,TFloat32.class, yTrue, yPred);
                       Operand<TFloat32> batchAccuracy = accuracy.apply(tf, TFloat32.class,yTrue, yPred);

                       float trainEpochAccuracy = 0;
                       float trainEpochLoss = 0;

                       // Load Batches
                       for (int i = 0; i < test.numBatches(); i++) {
                           runner = session.runner();
                           test.feedSessionRunner(runner, i);
                           runner.fetch(batchLoss);
                           runner.fetch(batchAccuracy);

                           List<Tensor> values = runner.run();
                           try (Tensor lossTensor = values.get(0);
                                Tensor accuracyTensor = values.get(1)) {
                               trainEpochAccuracy += accuracyTensor.asRawTensor().data().asFloats().getFloat(0) / test.numBatches();
                               trainEpochLoss += lossTensor.asRawTensor().data().asFloats().getFloat(0) / test.numBatches();
                           }
                       }

                       System.out.println("Test accuracy: " + trainEpochAccuracy + "  loss: " + trainEpochLoss);
                   }
               }
           }
       }
   }
}
