package matt.v1.mathexport

import matt.kjlib.jmath.simpleFactorial
import org.apache.commons.math3.special.Gamma
import java.math.BigDecimal
import kotlin.math.roundToInt

/*https://stackoverflow.com/questions/31539584/how-can-i-make-my-factorial-method-work-with-decimals-gamma*/
fun Double.generalizedFactorial(): Double {
  /*Gamma(n) = (n-1)! for integer n*/
  return Gamma.gamma(this + 1)
}

fun Double.generalizedFactorialOrSimpleIfInfOrNaN(): BigDecimal {
  /*Gamma(n) = (n-1)! for integer n*/
  println("getting gamma of ${this + 1}")
  return Gamma.gamma(this + 1).takeIf { !it.isInfinite() && !it.isNaN() }?.toBigDecimal() ?: roundToInt()
	  .simpleFactorial()
	  .toBigDecimal()
}