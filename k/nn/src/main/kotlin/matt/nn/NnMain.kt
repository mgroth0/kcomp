package matt.nn

//import matt.reflect.os
import matt.gui.app.GuiApp
import matt.kjlib.jmath.sigmoid
import matt.kjlib.jmath.sigmoidDerivative
import matt.klib.commons.os
import matt.klib.math.nextUnitDouble
import matt.nn.NeuralNetwork.Companion.INPUT_LENGTH
import matt.nn.model.Axon
import matt.nn.model.Neuron
import matt.nn.model.SumOfSquaresError
import matt.remote.host.ARemoteClassToMarkUsage
import matt.remote.host.Hosts
import matt.remote.runThisOnOM
import matt.remote.slurm.SRun
import matt.stream.applyEach
import kotlin.concurrent.thread
import kotlin.random.Random.Default.nextDouble
import kotlin.system.exitProcess


const val REMOTE = false

fun main() = GuiApp {
  println("yes i use ${Hosts.POLESTAR}")
  if (REMOTE) {
	thread {
	  Hosts.POLESTAR.ssh {
		runThisOnOM(srun = SRun(timeMin = 15))
	  }
	}
  }
  println("os:$os")
  val nn = NeuralNetwork(randomWeights = true)
  val stim = DoubleArray(INPUT_LENGTH) { nextDouble() }
  (0..5).forEach {
	val y = nn.feedforward(stim)
	println("y=${y[0]}")
	val actual = doubleArrayOf(0.5)
	val loss = SumOfSquaresError.compute(predicted = y, actual = actual)
	nn.backpropagate(predicted = y, actual = actual)
	println("loss:$loss")
  }
  exitProcess(0)
}.start()

class NeuralNetwork(
  randomWeights: Boolean = false,
  randomBiases: Boolean = false
) {
  companion object {
	const val INPUT_LENGTH = 10
	const val HIDDEN_LENGTH = INPUT_LENGTH*2
  }

  private val input = Array(INPUT_LENGTH) {
	Neuron(
	  bias = if (randomBiases) nextUnitDouble() else 0.0
	)
  }
  private val hidden = Array(HIDDEN_LENGTH) {
	Neuron(
	  bias = if (randomBiases) nextUnitDouble() else 0.0
	)
  }
  private val output = Neuron(
	bias = if (randomBiases) nextUnitDouble() else 0.0
  )

  init {
	input.applyEach {
	  hidden.forEach {
		axons += Axon(
		  output = it,
		  weight = if (randomWeights) (nextUnitDouble()) else 0.0
		)
		it.axons += Axon(
		  output = output,
		  weight = if (randomWeights) (nextUnitDouble()) else 0.0
		)
	  }
	}
  }


  fun feedforward(stimulus: DoubleArray): DoubleArray {
	require(stimulus.size == INPUT_LENGTH)

	input.forEach {
	  it.activation = 0.0
	}
	hidden.forEach {
	  it.activation = 0.0
	}
	output.activation = 0.0


	stimulus.mapIndexed { index, s ->
	  val ss = sigmoid(s + input[index].bias)
	  println("ss:$ss")
	  input[index].axons.map {
		it.output.activation += it.weight*ss
	  }
	}
	hidden.applyEach {
	  activation = sigmoid(activation + bias)
	  println("hiddenActivation:$activation")
	  axons.map {
		it.output.activation += it.weight*activation
	  }
	}
	output.activation = sigmoid(output.activation + output.bias)
	return doubleArrayOf(output.activation)
  }

  fun backpropagate(predicted: DoubleArray, actual: DoubleArray) {

	val negError = (actual[0] - predicted[0])

	hidden.forEach {
	  it.axons[0].weight += it.activation*
		  2*
		  negError*
		  sigmoidDerivative(it.axons[0].output.activation /*same as predicted[0]*/)
	}
	input.forEach { n ->
	  n.axons.forEach { axon ->
		axon.weight += n.activation*
			(2*
				negError*
				sigmoidDerivative(predicted[0])).let { d ->
			  output.axons.map { it.weight }.sumOf {
				it*d
			  }
			}*sigmoidDerivative(axon.output.activation)
	  }
	}

  }
}


