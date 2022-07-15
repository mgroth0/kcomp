package matt.nn.kotlindldemo

import matt.async.date.tic
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.activation.Activations.Tanh
import org.jetbrains.kotlinx.dl.api.core.initializer.Constant
import org.jetbrains.kotlinx.dl.api.core.initializer.GlorotNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.Zeros
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv2D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding.VALID
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.AvgPool2D
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.api.core.summary.logSummary
import org.jetbrains.kotlinx.dl.dataset.handler.NUMBER_OF_CLASSES
import org.jetbrains.kotlinx.dl.dataset.mnist


fun kotlindlDemo() {

  val t = tic(prefix = "kotlindlDemo")
  t.toc("starting")

  val EPOCHS = 3
  val TRAINING_BATCH_SIZE = 1000
  val NUM_CHANNELS = 1L
  val IMAGE_SIZE = 28L
  val SEED = 12L
  val TEST_BATCH_SIZE = 1000

  t.toc("defined constants")


  val lenet5Classic = Sequential.of(
	Input(
	  IMAGE_SIZE,
	  IMAGE_SIZE,
	  NUM_CHANNELS
	),
	Conv2D(
	  filters = 6,
	  kernelSize = intArrayOf(5, 5),
	  strides = intArrayOf(1, 1, 1, 1),
	  activation = Activations.Tanh,
	  kernelInitializer = GlorotNormal(SEED),
	  biasInitializer = Zeros(),
	  padding = ConvPadding.SAME
	),
	AvgPool2D(
	  poolSize = intArrayOf(1, 2, 2, 1),
	  strides = intArrayOf(1, 2, 2, 1),
	  padding = VALID
	),
	Conv2D(
	  filters = 16,
	  kernelSize = intArrayOf(5, 5),
	  strides = intArrayOf(1, 1, 1, 1),
	  activation = Activations.Tanh,
	  kernelInitializer = GlorotNormal(SEED),
	  biasInitializer = Zeros(),
	  padding = ConvPadding.SAME
	),
	AvgPool2D(
	  poolSize = intArrayOf(1, 2, 2, 1),
	  strides = intArrayOf(1, 2, 2, 1),
	  padding = ConvPadding.VALID
	),
	Flatten(), // 3136
	Dense(
	  outputSize = 120,
	  activation = Tanh,
	  kernelInitializer = GlorotNormal(SEED),
	  biasInitializer = Constant(0.1f)
	),
	Dense(
	  outputSize = 84,
	  activation = Activations.Tanh,
	  kernelInitializer = GlorotNormal(SEED),
	  biasInitializer = Constant(0.1f)
	),
	Dense(
	  outputSize = NUMBER_OF_CLASSES,
	  activation = Activations.Linear,
	  kernelInitializer = GlorotNormal(SEED),
	  biasInitializer = Constant(0.1f)
	)
  )

  t.toc("created modek")

  val (train, test) = mnist()

  t.toc("got data")

  lenet5Classic.use {
	t.toc("using model")
	it.compile(
	  optimizer = Adam(clipGradient = ClipGradientByValue(0.1f)),
	  loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
	  metric = Metrics.ACCURACY
	)
	t.toc("compiled model")

	it.logSummary()

	t.toc("got summary")

	it.fit(dataset = train, epochs = EPOCHS, batchSize = TRAINING_BATCH_SIZE)

	t.toc("fit mode")

	val accuracy = it.evaluate(dataset = test, batchSize = TEST_BATCH_SIZE).metrics[Metrics.ACCURACY]

	t.toc("got accuracy")

	println("Accuracy: $accuracy")
  }

  t.toc("finished")
}