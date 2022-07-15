package matt.nn.two

import matt.nn.two.mnistselectivity.mnistSelectivityDemo
import matt.remote.openmind.Polestar
import matt.remote.remoteOrLocal
import matt.remote.slurm.SRun
import org.tensorflow.ConcreteFunction
import org.tensorflow.Signature
import org.tensorflow.op.Ops
import org.tensorflow.types.TInt32


private val OMMachine = Polestar
val srun = if (OMMachine != Polestar) SRun(timeMin = 15) else null

fun main() = OMMachine.remoteOrLocal("k:nn:two:run", remote = true) {
  tfDemo()
  mnistSelectivityDemo()
}

fun tfDemo() {
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
}