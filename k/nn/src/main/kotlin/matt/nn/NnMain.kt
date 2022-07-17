package matt.nn

import matt.nn.kotlindldemo.kotlindlDemo
import matt.nn.model.NeuralNetwork
import matt.nn.model.NeuralNetwork.Companion.INPUT_LENGTH
import matt.nn.model.SumOfSquaresError
import kotlin.random.Random.Default.nextDouble


fun main() {
  bareBonesNNDemo()
  kotlindlDemo(epochs = 3)
}

fun bareBonesNNDemo() {
  val nn = NeuralNetwork(randomWeights = true)
  val stim = DoubleArray(INPUT_LENGTH) { nextDouble() }
  repeat(6) {
	val y = nn.feedforward(stim)
	println("y=${y[0]}")
	val actual = doubleArrayOf(0.5)
	val loss = SumOfSquaresError.compute(predicted = y, actual = actual)
	nn.backpropagate(predicted = y, actual = actual)
	println("loss:$loss")
  }
}