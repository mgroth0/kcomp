package matt.nn

import matt.nn.kotlindldemo.kotlindlDemo
import matt.nn.model.NeuralNetwork
import matt.nn.model.NeuralNetwork.Companion.INPUT_LENGTH
import matt.nn.model.SumOfSquaresError
import matt.remote.openmind.Polestar
import matt.remote.remoteOrLocal
import matt.remote.slurm.SRun
import kotlin.random.Random.Default.nextDouble


private val OMMachine = Polestar
val srun = if (OMMachine != Polestar) SRun(timeMin = 15) else null

fun main() = OMMachine.remoteOrLocal("k:nn:run", remote = true, srun = srun) {
  bareBonesNNDemo()
  kotlindlDemo()
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