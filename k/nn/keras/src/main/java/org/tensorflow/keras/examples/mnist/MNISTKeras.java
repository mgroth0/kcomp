//package org.tensorflow.keras.examples.mnist;
//
//import org.tensorflow.Graph;
//import org.tensorflow.data.GraphLoader;
//import org.tensorflow.keras.activations.Activations;
//import org.tensorflow.keras.datasets.MNIST;
//import org.tensorflow.keras.initializers.Initializers;
//import org.tensorflow.keras.layers.Dense;
//import org.tensorflow.keras.layers.Layers;
//import org.tensorflow.keras.losses.Losses;
//import org.tensorflow.keras.metrics.Metrics;
//import org.tensorflow.keras.models.Model;
//import org.tensorflow.keras.models.Sequential;
//import org.tensorflow.keras.optimizers.Optimizers;
//import org.tensorflow.op.Ops;
//import org.tensorflow.utils.Pair;
//
//public class MNISTKeras {
//    private static Model model;
//    private static Model.CompileOptions compileOptions;
//    private static Model.FitOptions fitOptions;
//
//    static {
//        // Define Neural Network Model
//
//        // Note: Layers can be constructed either from individual
//        //       Option.Builder classes, or from the static helper
//        //       methods defined in `Layers` which wrap the explicit builders
//        //       to decrease verbosity.
//        model = Sequential.of(
//                Layers.input(28, 28),
////                Layers.flatten(28 * 28),
//
//                // Using Layer Options Builder
//                new Dense(128, Dense.Options.builder()
//                        .setActivation(Activations.relu)
//                        .setKernelInitializer(Initializers.randomNormal)
//                        .setBiasInitializer(Initializers.zeros)
//                        .build()),
//
//                // Using static helper Layers.dense(...)
//                Layers.dense(10, Activations.softmax, Initializers.randomNormal, Initializers.zeros)
//        );
//
//        // Model Compile Configuration
//        compileOptions = Model.CompileOptions.builder()
//                .setOptimizer(Optimizers.sgd)
//                .setLoss(Losses.sparseCategoricalCrossentropy)
//                .addMetric(Metrics.accuracy)
//                .build();
//
//        // Model Training Loop Configuration
//        fitOptions = Model.FitOptions.builder()
//                .setEpochs(10)
//                .setBatchSize(100)
//                .build();
//    }
//
//
//    public static Model<Float> train() throws Exception {
//        try (Graph graph = new Graph()) {
//            // Create Tensorflow Ops Accessor
//            Ops tf = Ops.create(graph);
//
//            // Compile Model
//            model.compile(tf, Double.class, compileOptions);
//
//            Pair<GraphLoader<Float>, GraphLoader<Float>> loaders = MNIST.graphLoaders2D();
//            // GraphLoader objects contain AutoCloseable `Tensor` objects.
//            try (GraphLoader<Float> train = loaders.first();
//                 GraphLoader<Float> test = loaders.second()) {
//                // Fit model
//                model.fit(tf, train, test, fitOptions);
//            }
//        }
//
//        return model;
//    }
//
//    public static void main(String[] args) throws Exception {
//        train();
//    }
//}
