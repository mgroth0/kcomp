package matt.v1.exps

import matt.kjlib.jmath.assertRound
import matt.kjlib.jmath.mean
import matt.kjlib.jmath.toApfloat
import matt.kjlib.ranges.step
import matt.klib.log.warnOnce
import matt.v1.compcache.PPCUnit
import matt.v1.compcache.PreDNPopR
import matt.v1.gui.Figure
import matt.v1.gui.StatusLabel
import matt.v1.lab.ExpCategory.OTHER
import matt.v1.lab.ExpCategory.ROSENBERG
import matt.v1.lab.Experiment
import matt.v1.lab.Experiment.CoreLoop
import matt.v1.lab.Experiment.XVar.CONTRAST
import matt.v1.lab.Experiment.XVar.DIST_4_ATTENTION
import matt.v1.lab.Experiment.XVar.FT
import matt.v1.lab.Experiment.XVar.MASK
import matt.v1.lab.Experiment.XVar.PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.REL_STIM_ORIENTATION
import matt.v1.lab.Experiment.XVar.SIZE
import matt.v1.lab.Experiment.XVar.STIM_AND_PREF_ORIENTATION
import matt.v1.lab.Experiment.XVar.STIM_ORIENTATION
import matt.v1.lab.Fit.Gaussian
import matt.v1.lab.PoissonVar.FAKE1
import matt.v1.lab.PoissonVar.FAKE10
import matt.v1.lab.PoissonVar.FAKE5
import matt.v1.lab.PoissonVar.YES
import matt.v1.lab.SeriesCfg
import matt.v1.lab.petri.pop2D
import matt.v1.lab.rcfg.rCfg
import matt.v1.model.ASD_SIGMA_POOLING
import matt.v1.model.ATTENTION_SUPP_SIGMA_POOLING
import matt.v1.model.Stimulus
import matt.v1.model.tdDivNorm

/*(Rosenberg et al. 2015)*/
/*(Rosenberg et al. 2015 SI)*/

fun experiments(fig: Figure, statusLabel: StatusLabel): List<Experiment> {


  val noDN = SeriesCfg(
	label = "- D.N.",
	yExtractCustom = {
	  cell.cfgStim(
		cfgStim = seriesStim,
	  )
	}
  )
  val withDN = noDN.copy(
	label = "+ D.N.",
	yExtractCustom = {
	  cell.cfgStim(
		cfgStim = seriesStim,
		popR = PreDNPopR(seriesStim, attentionExp, pop)()
	  )
	}
  )
  val baseExp = Experiment(
	name = "3.B",
	title = "DN causes saturation with ↑ contrast",
	xlabel = "% Contrast",
	ylabel = "Neural Response % Maximum",
	fig = fig,
	statusLabel = statusLabel,
	xVar = CONTRAST,
	xStep = rCfg.F3B_STEP,
	series = listOf(noDN, withDN),
	normToMaxes = true,
	category = ROSENBERG
  )


  val exps = mutableListOf(baseExp)
  exps += baseExp.copy(
	name = "3.C",
	title = "DN allows suppression",
	xlabel = "Mask % Contrast",
	xMax = 50.0,
	xVar = MASK,
	xStep = rCfg.F3C_STEP
  )
  exps += baseExp.copy(
	name = "3.D",
	title = "DN causes tuning curve to peak at optimal size",
	xlabel = "Size (Degrees)",
	xMax = 10.0,
	xVar = SIZE,
	xStep = rCfg.F3D_STEP,
	xMin = rCfg.F3D_STEP,
	baseContrast = 1.0
  )

  val td = withDN.copy(label = "TD")
  val ascC = td.copy(
	label = "ASC c",
	popRcfg = { copy(divNorm = divNorm.copy(c = (tdDivNorm.c*0.75))) },
  )
  val popGainBySize = exps.last().copy(
	name = "4.C",
	title = "↓ D.N. in ASD causes ↑ the absolute pop. gain (matches b. data)",
	xMax = 6.05,
	xMin = 1.55,
	series = listOf(td, ascC),
	ylabel = "Population Gain",
	autoY = true,
	normToMaxes = false
  )
  exps += popGainBySize
  val contrastSensitivity = exps.last().copy(
	name = "4.D",
	title = "pop. gain from ↓ D.N. in ASD diverges from TD as contrast ↑ (matches b. data)",
	xMin = 0.0,
	xMax = 100.0,
	xVar = CONTRAST,
	xlabel = baseExp.xlabel,
	xStep = rCfg.F3B_STEP
  )
  exps += contrastSensitivity

  exps += baseExp.copy(
	name = "5.C",
	title = "dist. from center of attn. causes ↓ pop. gain and then ↑ (matches b. data)",
	xMin = 0.0,
	xMax = rCfg.X0_DIST_MAX,
	xVar = DIST_4_ATTENTION,
	xStep = rCfg.F5C_STEP,
	xlabel = "Distance (Degrees)",
	ylabel = exps.last().ylabel,
	troughShift = true,
	autoY = true,
	series = listOf(td, ascC),
	baseContrast = 1.0,
	normToMaxes = false
  )
  exps += exps.last().copy(
	name = "5.D",
	title = "pop. gain gradient from attn increases with ASD symptomatology (↑c) (matches b. data)",
	xMetaMin = 0.0,
	xMetaMax = 50.0,
	xlabel = "Gain Term % Decrease",
	ylabel = "Population Gain Gradient",
	metaGain = true,
	series = listOf(td) /*TD gets modified along AQ scale*/
  )

  val contrast1 =
	  td.copy(
		label = "${Experiment.CONTRAST1}%",


		stimCfg = {
		  it.copy(
			a = Experiment.CONTRAST1*(0.01)
		  )
		},


		)
  val contrast2 =
	  td.copy(
		label = "${Experiment.CONTRAST2}%",

		stimCfg = {
		  it.copy(
			a = Experiment.CONTRAST2*0.01
		  )
		}

	  )

  exps += baseExp.copy(
	name = "S1.A",
	xVar = PREF_ORIENTATION,
	xMin = 0.0,
	xMax = 179.0,
	xStep = rCfg.FS1_STEP,
	stimTrans = { copy(SF = 3.0, f = f.copy(t = 90.0)) },
	xlabel = "Preferred Orientation (Degrees)",
	ylabel = "Neural Response",
	autoY = true,
	series = listOf(contrast1, contrast2),
	title = "Definition of pop. gain",
	normToMaxes = false
	/*alpha is 0.075 or 0.2*/
	/*theta ranges from 0 to 180 in 0.25 steps*/
  )
  val PPC: CoreLoop.()->Double = {
	decode(
	  ftStim = seriesStim,
	  trialStim = seriesStim.copy(f = seriesStim.f.copy(t = 90.0)),
	)
  }
  exps += exps.last().copy(
	name = "S1.B.1",
	xVar = STIM_ORIENTATION,
	xlabel = "Stimulus Orientation (Degrees)",
	ylabel = "p(Θ|r)",
	autoY = true,
	title = "Probabilistic Population Code (PPC)",
	stimTrans = { copy(SF = 3.0) },
	series = exps.last().series.map { it.copy(yExtractCustom = PPC) }
  )
  exps += exps.last().copy(
	name = "S1.B.2",
	series = listOf(
	  contrast1.copy(label = "${contrast1.label} (Poisson)", poissonVar = YES, yExtractCustom = PPC),
	  contrast2.copy(label = "${contrast2.label} (Poisson)", poissonVar = YES, yExtractCustom = PPC)
	)
  )

  val ascV = td.copy(
	label = "ASC v",
	popRcfg = { copy(divNorm = divNorm.copy(v = 0.01)) },
  )
  val ascSS = td.copy(
	label = "ASC σs",
	popRcfg = { copy(sigmaPooling = ASD_SIGMA_POOLING) },
  )

  exps += popGainBySize.copy(
	name = "S2.A",
	title = "spatial suppression across ASD models",
	series = listOf(
	  ascC,
	  ascSS,
	  ascV,
	  td
	),
  )
  exps += contrastSensitivity.copy(
	name = "S2.B",
	title = "contrast sensitivity across ASD models",
	series = listOf(ascC, ascSS, ascV, td),
  )
  exps += exps.first { it.name == "5.C" }.copy(
	name = "S3.B",
	title = "5.C with low contrast",
	baseContrast = 0.1,
	troughShift = false
  )
  exps += exps.first { it.name == "5.D" }.copy(
	name = "S3.C",
	title = "5.D with low contrast",
	baseContrast = 0.1
  )
  exps += exps.first { it.name == "5.C" }.copy(
	name = "S3.D",
	title = "5.C with ↑D.N. pool size",
	troughShift = true,
	series = listOf(
	  td,
	  td.copy(
		label = "Increased σs",
		popRcfg = { copy(sigmaPooling = ATTENTION_SUPP_SIGMA_POOLING) },
	  )
	),
  )
  exps += exps.first { it.name == "5.C" }.copy(
	name = "S3.E",
	title = "5.C across v (semisaturation constant) changes",
	troughShift = false,
	series = listOf(
	  td.copy(label = "TD V1") /*same as TD with different label*/,
	  td.copy(label = "v = 20000 (2 in paper)",
		popRcfg = { copy(divNorm = divNorm.copy(v = 20_000.0)) }
	  ),
	  td.copy(
		label = "v = 40000 (4 in paper)",
		popRcfg = { copy(divNorm = divNorm.copy(v = 40_000.0)) },
	  ),
	  td.copy(
		label = "v = 60000 (6 in paper)",
		popRcfg = { copy(divNorm = divNorm.copy(v = 60_000.0)) },
	  )
	),
  )

  val priorSeriesBase = td.copy(
	label = "Flat",
	priorWeight = 0.0,
	popRcfg = { copy(divNorm = divNorm.copy(v = 0.000001)) }, /*DEBUG*/
  )
  val flatPrior = priorSeriesBase.copy(
	yExtractCustom = { cell.prior()() },
  )

  val weakPrior = flatPrior.copy(
	label = "Weak",
	priorWeight = tdDivNorm.c*.25 /*(2.5*10.0.pow(-5))*//* 7*10.0.pow(-5) *//*DEBUG*/ /**/
  )
  val strongPrior = flatPrior.copy(
	label = "Strong",
	priorWeight = tdDivNorm.c*.5 /*(5*10.0.pow(-5))*/  /*7.49*10.0.pow(-5)*/
  )
  val veryStrongPrior = tdDivNorm.c*.75 /*7.5*10.0.pow(-5)*/
  exps += baseExp.copy(
	name = "S4.A",
	title = "Bayesian Priors: Suppressive Field Gain Term",
	xMin = 0.0,
	xMax = rCfg.THETA_MAX - rCfg.CELL_THETA_STEP,
	xStep = 1.0,
	xVar = PREF_ORIENTATION,
	xlabel = "Preferred Orientation (Degrees)",
	ylabel = "c(Θ)",
	autoY = true,
	series = listOf(flatPrior, weakPrior, strongPrior),
	normToMaxes = false,
  )


  fun priorSeriesStimCfg(theta: Double?, plusX: Boolean = false): CoreLoop.(Stimulus)->Stimulus {
	return {
	  it.copy(
		SF = 3.0,
		gaussianEnveloped = false,
		a = 0.1,
		f = it.f.takeIf { theta == null && !plusX } ?: it.f.copy(t = theta!! + (if (plusX) x else 0.0))
	  )
	}
  }

  val flatPriorR = priorSeriesBase.copy(
	label = "Flat (45° stim)",
	stimCfg = priorSeriesStimCfg(45.0)
  )
  exps += exps.last().copy(
	name = "S4.B",
	title = "Bayesian Priors: Oblique effect is diminished in Autism",
	ylabel = "Neural Response",
	xStep = rCfg.CELL_THETA_STEP,
	series = listOf(
	  flatPriorR,
	  flatPriorR.copy(
		label = "Weak (45° stim)",
		priorWeight = weakPrior.priorWeight
	  ),
	  flatPriorR.copy(
		label = "Strong (45° stim)",
		priorWeight = strongPrior.priorWeight
	  ),
	  flatPriorR.copy(
		label = "Flat (90° stim)",
		stimCfg = priorSeriesStimCfg(90.0),
		priorWeight = flatPriorR.priorWeight
	  ),
	  flatPriorR.copy(
		label = "Weak (90° stim)",
		stimCfg = priorSeriesStimCfg(90.0),
		priorWeight = weakPrior.priorWeight
	  ),
	  flatPriorR.copy(
		label = "Strong (90° stim)",
		stimCfg = priorSeriesStimCfg(90.0),
		priorWeight = strongPrior.priorWeight
	  )
	)
  )

  val flatPriorPPCbase = flatPriorR.copy(
	label = "Flat",
	stimCfg = priorSeriesStimCfg(null)
  )
  val flatPriorPPC = flatPriorPPCbase.copy(
	yExtractCustom = {
	  warnOnce("debugging flatPriorPPC yExtract")
	  /*(1..(if (poissonVar == NONE) 1 else 20)).map {*/
	  //	  println("gonna look for cell with X=${seriesStim.X0} Y=${seriesStim.Y0} t=${seriesStim.t}")
	  /*decode*/
	  /*decodeBeforeGeoMean*/
	  /*warn("debugging 2 series commented out")*/
	  val d = decode(
		ftStim = seriesStim,
		trialStim = seriesStim,
	  )/*.mean()*/

	  /*.takeIf { it.count() == 1 }!!.maxOrNull()!!*/ // .mean()/*.first()*/
	  /*}.mean()*/
	  /*println("x=$x d=$d")*/
	  d.toDouble()

	}
  )

  exps += exps.last().copy(
	name = "S4.C.1",
	title = "Bayesian Priors: Oblique effect is diminished in Autism (PPCs)", /*Posterior Probability Distributions*/
	ylabel = "Max p(Θ|r)",
	xlabel = "Stimulus Orientation",
	xVar = STIM_ORIENTATION,
	series = listOf(
	  flatPriorPPC,
	  flatPriorPPC.copy(
		label = "Weak",
		priorWeight = weakPrior.priorWeight,
	  ),
	  flatPriorPPC.copy(
		label = "Strong",
		priorWeight = strongPrior.priorWeight
	  )
	),
	pop = pop2D,
	xStep = rCfg.CELL_THETA_STEP
  )

  exps += exps.last().copy(
	name = "S4.C.2",
	series = listOf(
	  flatPriorPPC.copy(
		label = "Flat (Poisson)",
		poissonVar = YES
	  ),
	  flatPriorPPC.copy(
		label = "Weak (Poisson)",
		priorWeight = weakPrior.priorWeight,
		poissonVar = YES
	  ),
	  flatPriorPPC.copy(
		label = "Strong (Poisson)",
		priorWeight = strongPrior.priorWeight,
		poissonVar = YES
	  ),
	)
  )
  exps += exps.last().copy(
	name = "S4.C.3",
	series = listOf(
	  flatPriorPPC.copy(
		label = "Flat (Fake1 Poisson)",
		poissonVar = FAKE1
	  ),
	  flatPriorPPC.copy(
		label = "Weak (Fake1 Poisson)",
		priorWeight = weakPrior.priorWeight,
		poissonVar = FAKE1
	  ),
	  flatPriorPPC.copy(
		label = "Strong (Fake1 Poisson)",
		priorWeight = strongPrior.priorWeight,
		poissonVar = FAKE1
	  )
	)
  )
  exps += exps.last().copy(
	name = "S4.C.4",
	series = listOf(
	  flatPriorPPC.copy(
		label = "Flat (Fake10 Poisson)",
		poissonVar = FAKE10
	  ),
	  flatPriorPPC.copy(
		label = "Weak (Fake10 Poisson)",
		priorWeight = weakPrior.priorWeight,
		poissonVar = FAKE10
	  ),
	  flatPriorPPC.copy(
		label = "Strong (Fake10 Poisson)",
		priorWeight = strongPrior.priorWeight,
		poissonVar = FAKE10
	  )
	)
  )

  exps += exps.last().copy(
	name = "S4.D",
	ylabel = "Baseline Activity",
	xlabel = "Preferred Orientation (Degrees)",
	title = "Bayesian Priors: Oblique effect is diminished in Autism (Pop. Gain)",
	xVar = STIM_AND_PREF_ORIENTATION,
	series = listOf(
	  flatPriorPPCbase.copy(
		label = "Baseline Activity",
		priorWeight = strongPrior.priorWeight
	  )
	)
  )

  fun CCWSeries(
	relThetaMid: Double,
	ccwTrials: Int
  ) = flatPriorPPC.copy(
	label = "$relThetaMid°",
	priorWeight = veryStrongPrior,
	stimCfg = priorSeriesStimCfg(relThetaMid, plusX = true),
	yExtractCustom = {
	  val substep = 1.0
	  (1..ccwTrials).map {
		val probs = (relThetaMid - (substep*9)..relThetaMid + substep*10 step substep)
			.mapIndexed { _, theta ->
			  (decode(
				ftStim = seriesStim.copy(f = seriesStim.f.copy(t = theta)),
				trialStim = seriesStim,
			  ) to if (theta > relThetaMid) 1.0 else 0.0)
			}
		(probs.maxByOrNull { it.first }!!.second)
	  }.mean()
	},
	line = true, /*while im waiting for fixes*/
	markers = true,
	fit = Gaussian
  )

  exps += baseExp.copy(
	name = "S5.1",
	xlabel = "Relative Orientation (Degrees)",
	ylabel = "Population CCW Response",
	title = "Psychometric Simulation of the Oblique Effect (Deterministic)",
	xMin = (-10.0),
	xMax = 10.0,
	xVar = REL_STIM_ORIENTATION,
	xStep = 1.0,
	series = listOf(
	  CCWSeries(relThetaMid = 90.0, ccwTrials = 1),
	  CCWSeries(relThetaMid = 45.0, ccwTrials = 1)
	),
	normToMaxes = false,
	autoY = true,
  )

  exps += exps.last().copy(
	name = "S5.2",
	title = exps.last().title.replace("Deterministic", "Poisson"),
	series = listOf(
	  CCWSeries(relThetaMid = 90.0, ccwTrials = 50).copy(poissonVar = YES),
	  CCWSeries(relThetaMid = 45.0, ccwTrials = 50).copy(poissonVar = YES)
	)
  )
  exps += exps.last().copy(
	name = "S5.3",
	title = exps.last().title.replace("Poisson", "Fake1 Poisson"),
	series = exps[exps.size - 2].series.map {
	  it.copy(
		poissonVar = FAKE1
	  )
	}
  )
  exps += exps.last().copy(
	name = "S5.4",
	title = exps.last().title.replace("Fake1 Poisson", "Fake5 Poisson"),
	series = exps.last().series.map {
	  it.copy(
		poissonVar = FAKE5
	  )
	}
  )
  exps += exps.last().copy(
	name = "S5.5",
	title = exps.last().title.replace("Fake5 Poisson", "Fake10 Poisson"),
	series = exps.last().series.map {
	  it.copy(
		poissonVar = FAKE10
	  )
	}
  )

  val p0 = SeriesCfg(
	label = "P=0",
	yExtractCustom = { PPCUnit(ft = x.toApfloat(), ri = x.toApfloat().assertRound())().toDouble() },
  )
  exps += baseExp.copy(
	name = "Ideal Poisson",
	title = "Ideal Neuronal Responses for PPCs",
	xlabel = "f(Θ):Mean Neural Response",
	ylabel = "PPC Unit Probability (informativeness)",
	autoY = true,
	normToMaxes = false,
	category = OTHER,
	xMin = 0.0,
	xMax = 5.0,
	xStep = 0.025,
	xVar = FT,
	series = listOf(
	  p0,
	  *(1..4).map { i ->
		p0.copy(
		  label = "P=$i",
		  yExtractCustom = { PPCUnit(ft = x.toApfloat(), ri = (x + i).toApfloat().assertRound())().toDouble() })
	  }.toTypedArray()
	)
  )

  return exps
}

