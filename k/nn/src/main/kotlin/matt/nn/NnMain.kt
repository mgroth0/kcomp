package matt.nn

import matt.klib.commons.os
import matt.klib.commons.thisMachine
import matt.klib.sys.Mac
import matt.nn.model.NeuralNetwork
import matt.nn.model.NeuralNetwork.Companion.INPUT_LENGTH
import matt.nn.model.SumOfSquaresError
import matt.remote.openmind.Polestar
import matt.remote.runOnOM
import matt.remote.slurm.SRun
import kotlin.concurrent.thread
import kotlin.random.Random.Default.nextDouble


val REMOTE = true
private val OMMachine = Polestar

fun main() {
  if (REMOTE && thisMachine is Mac) {
	thread {
	  println("here1")
	  OMMachine.ssh {
		println("here2")
		runOnOM(srun = SRun(timeMin = 15))
		println("here3")
	  }
	  println("here4")
	}
  } else {
	println("os:$os")
	bareBonesNNDemo()
	//	tfDemo()
  }
}

/*fun tfDemo() {
  fun dbl(tf: Ops): Signature {
	val x = tf.placeholder(TInt32::class.java)
	val dblX = tf.math.add(x, x)
	return Signature.builder().input("x", x).output("dbl", dblX).build()
  }
  ConcreteFunction.create { dbl(it) }.use { dbl ->
	TInt32.scalarOf(10).use { x ->
	  dbl.call(x).use { dblX ->
		println(
		  x.getInt().toString() + " doubled is " + (dblX as TInt32).getInt()
		)
	  }
	}
  }
}*/

fun bareBonesNNDemo() {
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
}