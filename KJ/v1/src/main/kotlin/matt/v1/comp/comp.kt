package matt.v1.comp

import matt.kjlib.jmath.getPoisson
import matt.kjlib.jmath.toApfloat
import matt.kjlib.jmath.toApint
import matt.klib.lang.err
import matt.v1.low.GPPCUnit
import matt.v1.low.LogPoissonPPCUnit
import matt.v1.low.PPCUnit
import org.apfloat.ApfloatMath
import java.math.RoundingMode.UNNECESSARY
import java.util.Random
import kotlin.math.roundToInt


enum class PoissonVar(val op: Double.()->Int, val gaussianOp: Double.()->Double) {
  NONE(op = {
	roundToInt()
  },
	gaussianOp = {
	  this
	}
  ),
  YES(op = {
	getPoisson()
  },
	gaussianOp = {
	  kotlin.math.max(
		this + Random().nextGaussian()*matt.v1.low.GPPCUnit.ftToSigma(this),
		0.0
	  )
	}),
  FAKE1(op = {
	roundToInt() + 1
  },
	gaussianOp = {
	  this + 1*matt.v1.low.GPPCUnit.ftToSigma(this)
	}),
  FAKE5(op = {
	roundToInt() + 5
  },
	gaussianOp = {
	  this + 5*matt.v1.low.GPPCUnit.ftToSigma(this)
	}),
  FAKE10(op = {
	roundToInt() + 10
  },
	gaussianOp = {
	  this + 10*matt.v1.low.GPPCUnit.ftToSigma(this)
	})
}


fun prob(
  ft: Double,
  preRI: Double,
  gaussian: Boolean,
  poissonVar: PoissonVar,
  log: Boolean
) =
  if (log && gaussian) err("todo?")
  else if (log) LogPoissonPPCUnit(
	ft = ft,
	ri = poissonVar.op(preRI)
  ).findOrCompute()
  else if (!gaussian) {
	PPCUnit(
	  ft = ft.toApfloat(),
	  ri = ApfloatMath.round(
		poissonVar.op(preRI).toApint(),
		20,
		UNNECESSARY
	  ).truncate()
	).findOrCompute().toDouble()
  } else {
	GPPCUnit(
	  ft = ft,
	  ri = poissonVar.gaussianOp(preRI)
	).findOrCompute()
  }


enum class Fit {
  Gaussian
}