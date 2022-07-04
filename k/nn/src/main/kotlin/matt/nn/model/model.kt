package matt.nn.model

import matt.klib.math.sq

data class Neuron(val bias: Double = 0.0) {
  val axons = mutableListOf<Axon>()
  var activation = 0.0
}

class Axon(
  val output: Neuron,
  var weight: Double = 1.0
)

abstract class LossFunction {
  abstract fun compute(predicted: DoubleArray, actual: DoubleArray): Double
}

object SumOfSquaresError: LossFunction() {
  override fun compute(predicted: DoubleArray, actual: DoubleArray): Double {
	require(predicted.size == actual.size)
	return predicted.mapIndexed { i, p -> (actual[i] - p).sq() }.sum()
  }
}