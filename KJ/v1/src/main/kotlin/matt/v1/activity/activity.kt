package matt.v1.activity

import matt.caching.compcache.ComputeInput
import matt.caching.compcache.UpdaterComputeInput
import matt.caching.parAssociateWith
import matt.kjlib.jmath.e
import matt.kjlib.jmath.point.BasicPoint
import matt.kjlib.lang.err
import matt.klib.dmap.withStoringDefault
import matt.klib.math.sq
import matt.v1.activity.AttentionAmp.Companion.ATTENTION_CENTER
import matt.v1.lab.petri.Population
import matt.v1.low.Norm
import matt.v1.low.Weight
import matt.v1.low.normDistTo
import matt.v1.low.normTo
import matt.v1.model.Input
import matt.v1.model.RawInput
import matt.v1.model.Stimulus
import matt.v1.model.activity.ActivityConfig
import matt.v1.model.activity.DYNAMIC_BASELINE_B
import matt.v1.model.activity.WeightConfig.SigmaPooling
import matt.v1.model.activity.WeightConfig.Uniform
import matt.v1.model.complexcell.ComplexCell
import org.apfloat.Apfloat
import org.apfloat.Apint
import kotlin.math.pow


data class Stimulation(
  val activityConfig: ActivityConfig,
  val cell: ComplexCell,
  val input: Input,
  val popR: Map<ComplexCell, Response>? = null,
  val attention: Boolean = false,
  val lastResponse: Response? = null,
  val ti: Apint? = null,
  val h: Double? = null,
): ComputeInput<Response>() {
  override fun compute(): Response {
	@Suppress("LocalVariableName") var G: Double? = null
	if (ti == Apfloat.ZERO) return Response(
	  R = 0.0, G_S = G, debugSinR = 0.0
	)
	@Suppress("LocalVariableName") val divNormS = if (popR == null) null else {
	  val realH = h ?: 1.0
	  val dnCfg = activityConfig.cfgDN!!
	  val S = popR.entries.sumOf {
		(((dnCfg.weightCfg as? Uniform)?.w
		  ?: WEIGHT_CACHE[cell][(dnCfg.weightCfg as SigmaPooling).s]!![it.key]!!)*it.value.R /*W * r*/)
	  }
	  if (ti == null) S else if (ti == Apint.ZERO) 0.0 else lastResponse!!.G_S!! + realH*(-lastResponse.G_S!! + S)
	}
	var sinR: Float? = null
	var cosR: Float? = null
	val input = (input as? RawInput)?.v ?: run {
	  sinR = cell.sinCell.stimulate(input as Stimulus)
	  cosR = cell.cosCell.stimulate(input)
	  val inp = sinR!!.sq() + cosR!!.sq()
	  inp
	}
	G = divNormS?.toDouble()
	var r =
	  ((if (ti != null) DYNAMIC_BASELINE_B.toDouble() else activityConfig.baselineActivityDC) + input.toDouble()).let {
		if (popR == null) {
		  if (attention) {
			it*AttentionAmp(
			  norm = (input as Stimulus).normTo(ATTENTION_CENTER)
			).findOrCompute()
		  } else it
		} else if (ti == null) popR[cell]!!.R /*why... its the same right?*/ else it
	  }.let {

		val pw = popR?.let { activityConfig.baseDivNorm }?.copy(
		  D = it,
		  S = divNormS,
		)?.findOrCompute() ?: it
		pw
	  }
	if (ti != null) {
	  val riLast = lastResponse!!.R
	  r = riLast + (h!!*(-riLast + r))
	}
	val resp = Response(
	  R = r.toDouble(), G_S = G, debugSinR = sinR!!.toDouble(),
	  debugCosR = cosR!!.toDouble()
	)
	if (h != null) {
	  err("lastResponse = resp")
	}
	return resp
  }
}

/*TODO: is there some redundancy here since the weight will be the same in both directions for cell1 vs cell2?*/
private val WEIGHT_CACHE = mutableMapOf<
	ComplexCell,
	MutableMap<
		Double,
		MutableMap<
			ComplexCell,
			Double
			>
		>
	>().withStoringDefault { cell1 ->
  mutableMapOf<Double, MutableMap<ComplexCell, Double>>().withStoringDefault { sigmaPooling ->
	mutableMapOf<ComplexCell, Double>().withStoringDefault { cell2 ->
	  Weight(norm = cell1.normDistTo(cell2), sigmaPool = sigmaPooling)()
	}
  }
}

data class Response(
  val R: Double,
  val G_S: Double?,
  val debugSinR: Double? = null,
  val debugCosR: Double? = null
)


//data class PopulationResponse(
//  val m: Map<ComplexCell, Response>,
//  val sigmaPooling: Double,
//  val divNorm: DivNorm
//): Map<ComplexCell, Response> by m {
//  override fun toString() = toStringBuilder(::sigmaPooling, ::divNorm)
//}


data class DivNorm(
  val D: Double?,
  val c: Double, /*Suppressive Field Gain*/
  val v: Double, /*SemiSaturation Constant*/
  val S: Double? /*Suppressive Field*/
): ComputeInput<Double>() {
  override fun compute() = D!!/(v + c*S!!)
}


data class MaybePreDNPopR(
  val activityConfig: ActivityConfig,
  val input: Input,
  val attention: Boolean = false,
  val pop: Population,
  val ti: Apint? = null,
  val h: Double? = null,
  val lastPopR: Map<ComplexCell, Response>? = null,
): UpdaterComputeInput<ComplexCell, Response>() {
  override fun futureMapBuilder() = pop.complexCells.asSequence().parAssociateWith {
	Stimulation(
	  activityConfig = activityConfig,
	  cell = it,
	  input = input,
	  attention = attention,
	  popR = lastPopR,
	  ti = ti,
	  h = h,
	)()
  }
}


data class AttentionAmp(
  val norm: Norm
): ComputeInput<Double>() {

  companion object {
	private val G = 7.0
	private val SIGMA_ATTENTION = 2.0
	val ATTENTION_CENTER = BasicPoint(0, 0)
  }

  override fun compute() = 1 + G*e.pow(-((norm.findOrCompute().sq())/(2*SIGMA_ATTENTION.sq())))
}

class DynamicActivation {
  val lastResponseMap = mutableMapOf<ComplexCell, Response>().withStoringDefault {
	Response(R = 0.0, G_S = 0.0, debugSinR = 0.0)
  }
}