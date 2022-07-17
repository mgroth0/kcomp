package matt.nn.two.mnistselectivity

import matt.async.date.tic
import matt.file.commons.DATA_FOLDER
import org.tensorflow.Graph
import org.tensorflow.keras.activations.Activations
import org.tensorflow.keras.data.GraphLoader
import org.tensorflow.keras.datasets.MNIST
import org.tensorflow.keras.initializers.Initializers
import org.tensorflow.keras.layers.Dense
import org.tensorflow.keras.layers.Layers
import org.tensorflow.keras.losses.Losses
import org.tensorflow.keras.metrics.Metrics
import org.tensorflow.keras.models.Model
import org.tensorflow.keras.models.Sequential
import org.tensorflow.keras.optimizers.Optimizers
import org.tensorflow.op.Ops
import org.tensorflow.types.TFloat32


/*ORIG: c*/
/*MINE (just added download line): https://colab.research.google.com/drive/1vGnaYrjaNdGTiPZ2msZDJ-WEzyNdlBWb*/
private val PRIV_DATA_FOLD = DATA_FOLDER + "MNIST_selectivity"


fun mnistSelectivityDemo() {
  val t = tic(prefix = "MNIST SELECTIVITY")
  t.toc("starting MNIST selectivity demo")

  @Suppress("UNUSED_VARIABLE") val sampleDataFold = PRIV_DATA_FOLD + "sample_data"
  @Suppress("UNUSED_VARIABLE") val robustnessFold = PRIV_DATA_FOLD + "Robustness"




  //  sampleDataFold.mkdirs()
  //  if (robustnessFold.doesNotExist) {
  //	t.toc("cloning robustness")
  //	shell("git", "clone", "https://github.com/kimvc7/Robustness.git", workingDir = PRIV_DATA_FOLD, debug = true)
  //	t.toc("finished cloning robustness")
  //  }
  //  t.toc("generating datasets")
  //  shell("/usr/bin/python3", "main.py", "--run=config", "--config=generate_datasets", workingDir = robustnessFold)
  //  t.toc("finished generating datasets")


  /*MNIST.graphLoaders()*/


  val model = Sequential.of(
	TFloat32::class.java,
	Layers.input(28, 28),
	Layers.flatten(),  /*28 * 28*/

	// Using Layer Options Builder
	Dense(
	  128, Dense.Options.builder()
		.setActivation(Activations.relu)
		.setKernelInitializer(Initializers.randomNormal)
		.setBiasInitializer(Initializers.zeros)
		.build()
	),

	// Using static helper Layers.dense(...)
	Layers.dense<TFloat32>(10, Activations.softmax, Initializers.randomNormal, Initializers.zeros)
  )


  // Model Compile Configuration
  val compileOptions = Model.CompileOptions.builder()
	.setOptimizer(Optimizers.sgd)
	.setLoss(Losses.sparseCategoricalCrossentropy)
	.addMetric(Metrics.accuracy)
	.build();

  // Model Training Loop Configuration
  val fitOptions = Model.FitOptions.builder()
	.setEpochs(10)
	.setBatchSize(100)
	.build();


  Graph().use { graph ->
	// Create Tensorflow Ops Accessor
	val tf: Ops = Ops.create(graph)

	// Compile Model
	model.compile(tf, compileOptions)


	val loaders: org.tensorflow.utils.Pair<GraphLoader<TFloat32>, GraphLoader<TFloat32>> = MNIST.graphLoaders2D()
	loaders.first().use { train ->
	  loaders.second().use { test ->
		// Fit model
		model.fit(tf, train, test, fitOptions)

	  }
	}
  }

  //  return model


  /*model.fit()*/

  t.toc("finished MNIST selectivity demo")
}