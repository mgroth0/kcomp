package matt.v1.exps

import matt.kjlib.jmath.UnitType.DEGREES
import matt.kjlib.jmath.UnitType.PERCENT
import matt.kjlib.jmath.point.PointDim.X
import matt.kjlib.jmath.point.PointDim.Y
import matt.kjlib.jmath.point.normalizeToMax
import matt.kjlib.jmath.point.showAsPercent
import matt.kjlib.ranges.step
import matt.v1.exps.expmodels.varyingStimExp
import matt.v1.figmodels.AxisConfig
import matt.v1.lab.Experiment


fun experiments(): List<Experiment> {

  val degreesXStep = 0.1
  val degreesXMax = 10.0

  val baseXConfig = AxisConfig(
	min = 0.0,
	max = 100.0,
	unit = PERCENT,
	label = "% Contrast",
	postProcessOp = {
	  it.showAsPercent(dim = X)
	}
  )
  val degreesXConfig = AxisConfig(
	label = "Size",
	unit = DEGREES,
	min = degreesXStep,
	max = degreesXMax,
  )
  val percentYConfig = AxisConfig(
	label = "Neural Response % Maximum",
	unit = PERCENT,
	min = 0.0,
	max = 100.0,
	postProcessOp = {
	  it.normalizeToMax(Y)
	}
  )

  val exps = mutableListOf(
	varyingStimExp(
	  expID = 3 to 'B',
	  title = "DN causes saturation with ↑ contrast",
	  xAxisConfig = baseXConfig,
	  yAxisConfig = percentYConfig,
	  inputRange = 0.0..1.0 step 0.01,
	  xStimOp = { x -> copy(a = x) }
	),
	varyingStimExp(
	  expID = 3 to 'C',
	  title = "DN allows suppression",
	  xAxisConfig = baseXConfig.copy(
		max = 50.0,
		label = "Mask % Contrast"
	  ),
	  yAxisConfig = percentYConfig,
	  inputRange = 0.0..0.5 step 0.005,
	  xStimOp = { x ->
		this withMask copy(
		  a = x,
		  o = o.orth
		)
	  }
	),
	varyingStimExp(
	  expID = 3 to 'D',
	  title = "DN causes tuning curve to peak at optimal size",
	  xAxisConfig = degreesXConfig,
	  yAxisConfig = percentYConfig,
	  inputRange = degreesXStep..degreesXMax step degreesXStep,
	  xStimOp = { copy(s = it) }
	)
  )


//  /*STUPID STUPID STUPID*/
//  warn("STUPID STUPID STUPID")/*val td = withDN.copy(label = "TD")*/
//  val td = matt.v1.figmodels.SeriesCfg(
//	label = "STUPID"
//  )

//
//  val ascC = td.copy(
//	label = "ASC c",
//	popRcfg = {
//	  copy(
//		divNorm = divNorm.copy(
//		  c = ARI_TD_DN_CFG.gainC.toApfloat()*0.75f
//		)
//	  )
//	},
//  )

  val popGainBySize = varyingStimExp(
	expID = 4 to 'C',
	title = "↓ D.N. in ASD causes ↑ the absolute pop. gain (matches b. data)",
	xAxisConfig = degreesXConfig.copy(
	  min = 1.55,
	  max = 6.05,
	),
	inputRange = degreesXStep..degreesXMax step degreesXStep,
	yAxisConfig = AxisConfig(
	  min = 20,
	  max = 45,
	  label = "Population Gain",
	  unit = null
	),

	/*series = listOf(td, ascC),*/ /*just check or uncheck series?*/


	xStimOp = { copy(s = it) }
  )
  exps += popGainBySize
  return exps


  //
  //
  //
  //
  //
  //  fun CoreLoop.prior(
  //	cell: ComplexCell
  //  ) = BayesianPriorC(
  //	c0 = ROSENBERG_TD_C.toApfloat(), w0 = priorW!!, t = cell.tDegrees.toApfloat()
  //  )
  //
  //  fun CoreLoop.cfgStim(
  //	cell: ComplexCell,
  //	cfgStim: Stimulus,
  //	popR: PopulationResponse? = null,
  //  ) = Stimulation(
  //	cell = cell,
  //	stim = cfgStim,
  //	popR = popR?.copy(
  //	  divNorm = when {
  //		metaC != null -> popR.divNorm.copy(c = metaC!!)
  //		priorW != null -> popR.divNorm.copy(c = prior(cell)())
  //		else -> popR.divNorm
  //	  }
  //	)?.popRCfg(),
  //	attention = outer.useAttentionMask,
  //	uniformW = outer.uniformW,
  //	rawInput = outer.rawInput,
  //	ti = ti,
  //	h = h,
  //  ).findOrCompute()
  //
  //


  //  val contrastSensitivity = exps.last().copy(
  //	name = "4.D", title = "pop. gain from ↓ D.N. in ASD diverges from TD as contrast ↑ (matches b. data)", xMin = 0.0,
  //	xMax = 100.0, xOp = baseExp.xOp, xlabel = baseExp.xlabel, xStep = baseExp.xStep
  //  )
  //  exps += contrastSensitivity
  //
  //  exps += baseExp.copy(
  //	name = "5.C",
  //	title = "dist. from center of attn. causes ↓ pop. gain and then ↑ (matches b. data)",
  //	xMin = 0.0,
  //	xMax = baseExp.attnX0DistMax.toDouble(),
  //	xOp = {
  //	  err("baseContrast = 1.0.toApfloat(),")
  //	  cell = outer.pop.complexCells.filter {
  //		it.tDegrees == 0.0 && it.SF == outer.popCfg.cellSFmin.toDouble() && it.Y0 == 0.0 && it.X0 > x.toDouble()
  //	  }.minByOrNull { it.X0 }!!
  //	},
  //	useAttentionMask = true,
  //	xStep = baseExp.f5cStep.toDouble(),
  //	xlabel = "Distance (Degrees)",
  //	ylabel = exps.last().ylabel,
  //	troughShift = true,
  //	autoY = true,
  //	series = listOf(td, ascC),
  //
  //	normToMinMax = false,
  //	normToMax = false,
  //	popCfg = baseExp.popCfg.copy(
  //	  cellX0AbsMinmax = 15.0
  //	)
  //  )
  //
  //
  //  exps += exps.last().copy(
  //	name = "5.D", title = "pop. gain matt.v1.mathexport.point.getGradient from attn increases with ASD symptomatology (↑c) (matches b. data)",
  //	xMetaMin = 0.0.toApfloat(), xMetaMax = 50.0.toApfloat(), xlabel = "Gain Term % Decrease",
  //	ylabel = "Population Gain Gradient", metaGain = true, series = listOf(td) /*TD gets modified along AQ scale*/
  //  )
  //
  //  val contrast1 = td.copy(
  //	label = "${Experiment.CONTRAST1}%",
  //
  //
  //	stimCfg = {
  //	  it.copy(
  //		a = Experiment.CONTRAST1*(0.01)
  //	  )
  //	},
  //
  //
  //	)
  //  val contrast2 = td.copy(label = "${Experiment.CONTRAST2}%",
  //
  //	stimCfg = {
  //	  it.copy(
  //		a = Experiment.CONTRAST2*0.01
  //	  )
  //	}
  //
  //  )
  //
  //  val xThetaExample = baseExp.copy(
  //	name = "S1.A", xOp = {
  //	  cell = outer.pop.complexCells.filter {
  //		it.SF == outer.popCfg.cellSFmin.toDouble() && it.Y0 == 0.0 && it.X0 == 0.0 && it.tDegrees > x.toDouble()
  //	  }.minByOrNull { it.tDegrees }!!
  //	  stim = stim.copy(
  //		SF = 3.0, f = stim.f.copy(tDegrees = 90.0)
  //	  )
  //	}, xMin = 0.0, xMax = 179.0, xStep = baseExp.fs1Step.toDouble(), xlabel = "Preferred Orientation (Degrees)",
  //	ylabel = "Neural Response", autoY = true, series = listOf(contrast1, contrast2), title = "Definition of pop. gain",
  //	normToMinMax = false    /*alpha is 0.075 or 0.2*/    /*theta ranges from 0 to 180 in 0.25 steps*/
  //  )
  //  exps += xThetaExample
  //
  //  fun CoreLoop.decodeBeforeGeoMean(
  //	ftStim: Stimulus,
  //	trialStim: Stimulus,
  //  ) = outer.pop.complexCells.toList().mapIndexed { i, c ->
  //	if (i%10 == 0) globalStatusLabel!!.counters["decoding cell"] = i + 1 to outer.pop.complexCells.size
  //	prob(
  //	  ft = cfgStim(
  //		cell = c, ftStim, popR = MaybePreDNPopR(
  //		  stim = ftStim,
  //		  attention = outer.useAttentionMask,
  //		  pop = outer.pop,
  //		  uniformW = outer.uniformW,
  //		  rawInput = outer.rawInput,
  //		  sigmaPooling = outer.pop.cfg.activityConfig.baseSigmaPooling,
  //		  divNorm = outer.pop.cfg.activityConfig.baseDivNorm
  //		)()
  //	  ).R, preRI = cfgStim(
  //		cell = c,
  //		cfgStim = trialStim,
  //		popR = MaybePreDNPopR(
  //		  stim = trialStim,
  //		  attention = outer.useAttentionMask,
  //		  pop = outer.pop,
  //		  uniformW = outer.uniformW,
  //		  rawInput = outer.rawInput,
  //		  sigmaPooling = outer.pop.cfg.activityConfig.baseSigmaPooling,
  //		  divNorm = outer.pop.cfg.activityConfig.baseDivNorm
  //		)()
  //	  ).R.toDouble(), gaussian = USE_GPPC, poissonVar = poissonVar, log = outer.logForm
  //	)
  //  }
  //
  //  fun CoreLoop.decode(
  //	ftStim: Stimulus,
  //	trialStim: Stimulus,
  //  ) = (1..(if (poissonVar == YES) outer.decodeCount else 1)).map {
  //	decodeBeforeGeoMean(
  //	  ftStim = ftStim, trialStim = trialStim
  //	).run {
  //	  if (outer.logForm) sum() else logSum()
  //	}
  //  }.mean()
  //
  //  val PPC: CoreLoop.()->Double = {
  //	decode(
  //	  ftStim = seriesStim,
  //	  trialStim = seriesStim.copy(f = seriesStim.f.copy(tDegrees = 90.0)),
  //	)
  //  }
  //  val stimOrientationXOpExample = exps.last().copy(
  //	name = "S1.B.1", xOp = {
  //	  stim = stim.copy(f = stim.f.copy(tDegrees = x.toDouble())).copy(SF = 3.0)
  //	}, xlabel = "Stimulus Orientation (Degrees)", ylabel = "p(Θ|r)", title = "Probabilistic Population Code (PPC)",
  //	series = exps.last().series.map { (it as matt.v1.figmodels.matt.v1.figmodels.SeriesCfg).copy(yExtractCustom = PPC) }, autoY = false,
  //	normToMinMax = true /*Not exactly what Rosenberg did, but they did do some sort of normalization that they didn't explicitly document according to Jon*/
  //  )
  //  exps += stimOrientationXOpExample
  //  exps += exps.last().copy(
  //	name = "S1.B.2", series = listOf(
  //	  contrast1.copy(label = "${contrast1.label} (Poisson)", poissonVar = YES, yExtractCustom = PPC),
  //	  contrast2.copy(label = "${contrast2.label} (Poisson)", poissonVar = YES, yExtractCustom = PPC)
  //	), autoY = true, normToMinMax = false
  //  )
  //
  //  val ascV = td.copy(
  //	label = "ASC v",
  //	popRcfg = { copy(divNorm = divNorm.copy(v = 0.01.toApfloat())) },
  //  )
  //  val ascSS = td.copy(
  //	label = "ASC σs",
  //	popRcfg = { copy(sigmaPooling = ASD_SIGMA_POOLING) },
  //  )
  //
  //  exps += popGainBySize.copy(
  //	name = "S2.A",
  //	title = "spatial suppression across ASD models",
  //	series = listOf(
  //	  ascC, ascSS, ascV, td
  //	),
  //  )
  //  exps += contrastSensitivity.copy(
  //	name = "S2.B",
  //	title = "contrast sensitivity across ASD models",
  //	series = listOf(ascC, ascSS, ascV, td),
  //  )
  //  exps += exps.first { it.name == "5.C" }.copy(
  //	name = "S3.B", title = "5.C with low contrast", xOp = {
  //	  err("baseContrast = 0.1.toApfloat(),")
  //	}, troughShift = false
  //  )
  //  exps += exps.first { it.name == "5.D" }.copy(name = "S3.C", title = "5.D with low contrast", xOp = {
  //	err("baseContrast = 0.1.toApfloat(),")
  //  })
  //  exps += exps.first { it.name == "5.C" }.copy(
  //	name = "S3.D",
  //	title = "5.C with ↑D.N. pool size",
  //	troughShift = true,
  //	series = listOf(
  //	  td, td.copy(
  //		label = "Increased σs",
  //		popRcfg = { copy(sigmaPooling = ATTENTION_SUPP_SIGMA_POOLING) },
  //	  )
  //	),
  //  )
  //  exps += exps.first { it.name == "5.C" }.copy(
  //	name = "S3.E",
  //	title = "5.C across v (semisaturation constant) changes",
  //	troughShift = false,
  //	series = listOf(
  //	  td.copy(label = "TD V1") /*same as TD with different label*/,
  //	  td.copy(label = "v = 20000 (2 in paper)", popRcfg = { copy(divNorm = divNorm.copy(v = 20_000.0.toApfloat())) }),
  //	  td.copy(
  //		label = "v = 40000 (4 in paper)",
  //		popRcfg = { copy(divNorm = divNorm.copy(v = 40_000.0.toApfloat())) },
  //	  ), td.copy(
  //		label = "v = 60000 (6 in paper)",
  //		popRcfg = { copy(divNorm = divNorm.copy(v = 60_000.0.toApfloat())) },
  //	  )
  //	),
  //  )
  //
  //  val priorSeriesBase = td.copy(
  //	label = "Flat",
  //	priorWeight = 0.0.toApfloat(),
  //	popRcfg = { copy(divNorm = divNorm.copy(v = 0.000001.toApfloat())) }, /*DEBUG*/
  //  )
  //
  //
  //  val flatPrior = priorSeriesBase.copy(
  //	yExtractCustom = { prior(cell)() },
  //  )
  //
  //  val weakPrior = flatPrior.copy(
  //	label = "Weak",
  //	priorWeight = ROSENBERG_TD_C.toApfloat()*.25f /*(2.5*10.0.pow(-5))*//* 7*10.0.pow(-5) *//*DEBUG*/ /**/
  //  )
  //  val strongPrior = flatPrior.copy(
  //	label = "Strong", priorWeight = ROSENBERG_TD_C.toApfloat()*.5f /*(5*10.0.pow(-5))*/  /*7.49*10.0.pow(-5)*/
  //  )
  //  val veryStrongPrior = ROSENBERG_TD_C.toApfloat()*.75f /*7.5*10.0.pow(-5)*/
  //  val expThetaMax = pop2D.prefThetaMax - pop2D.cellPrefThetaStep
  //  exps += baseExp.copy(
  //	name = "S4.A",
  //	title = "Bayesian Priors: Suppressive Field Gain Term",
  //	xMin = 0.0,
  //	xMax = expThetaMax.toDouble(),
  //	xStep = 1.0,
  //	xOp = xThetaExample.xOp,
  //	xlabel = "Preferred Orientation (Degrees)",
  //	ylabel = "c(Θ)",
  //	autoY = true,
  //	series = listOf(flatPrior, weakPrior, strongPrior),
  //	normToMinMax = false,
  //  )
  //
  //
  //  fun priorSeriesStimCfg(theta: Float?, plusX: Boolean = false): CoreLoop.(Stimulus)->Stimulus {
  //	return {
  //	  it.copy(SF = 3.0, gaussianEnveloped = false, a = 0.1, f = it.f.takeIf { theta == null && !plusX } ?: it.f.copy(
  //		tDegrees = theta!! + (if (plusX) x.toDouble() else 0.0)
  //	  ))
  //	}
  //  }
  //
  //  val flatPriorR = priorSeriesBase.copy(
  //	label = "Flat (45° stim)", stimCfg = priorSeriesStimCfg(45.0f)
  //  )
  //  exps += exps.last().copy(
  //	name = "S4.B", title = "Bayesian Priors: Oblique effect is diminished in Autism", ylabel = "Neural Response",
  //	xStep = pop2D.cellPrefThetaStep, series = listOf(
  //	  flatPriorR, flatPriorR.copy(
  //		label = "Weak (45° stim)", priorWeight = weakPrior.priorWeight
  //	  ), flatPriorR.copy(
  //		label = "Strong (45° stim)", priorWeight = strongPrior.priorWeight
  //	  ), flatPriorR.copy(
  //		label = "Flat (90° stim)", stimCfg = priorSeriesStimCfg(90.0f), priorWeight = flatPriorR.priorWeight
  //	  ), flatPriorR.copy(
  //		label = "Weak (90° stim)", stimCfg = priorSeriesStimCfg(90.0f), priorWeight = weakPrior.priorWeight
  //	  ), flatPriorR.copy(
  //		label = "Strong (90° stim)", stimCfg = priorSeriesStimCfg(90.0f), priorWeight = strongPrior.priorWeight
  //	  )
  //	)
  //  )
  //
  //  val flatPriorPPCbase = flatPriorR.copy(
  //	label = "Flat", stimCfg = priorSeriesStimCfg(null)
  //  )
  //  val flatPriorPPC = flatPriorPPCbase.copy(yExtractCustom = {
  //	warnOnce(
  //	  "debugging flatPriorPPC yExtract"
  //	)
  //	val d1 = decode(
  //	  ftStim = seriesStim,
  //	  trialStim = seriesStim,
  //	)
  //
  //	val d2 = decode(
  //	  ftStim = seriesStim.copy(
  //		f = seriesStim.f.copy(
  //		  tDegrees = if (seriesStim.f.tDegrees == expThetaMax.toDouble()) seriesStim.f.tDegrees - pop2D.cellPrefThetaStep else seriesStim.f.tDegrees + pop2D.cellPrefThetaStep.toDouble()
  //		)
  //	  ),
  //	  trialStim = seriesStim,
  //	)
  //	d1 - d2
  //  })
  //
  //  exps += exps.last().copy(
  //	name = "S4.C.1",
  //	title = "Bayesian Priors: Oblique effect is diminished in Autism (PPCs)", /*Posterior Probability Distributions*/
  //	ylabel = "Max p(Θ|r)", xlabel = "Stimulus Orientation", xOp = stimOrientationXOpExample.xOp, series = listOf(
  //	  flatPriorPPC, flatPriorPPC.copy(
  //		label = "Weak",
  //		priorWeight = weakPrior.priorWeight,
  //	  ), flatPriorPPC.copy(
  //		label = "Strong", priorWeight = strongPrior.priorWeight
  //	  )
  //	), popCfg = pop2D, xStep = pop2D.cellPrefThetaStep
  //  )
  //
  //
  //  exps += exps.last().copy(
  //	name = "S4.C.2", series = listOf(
  //	  flatPriorPPC.copy(
  //		label = "Flat (Poisson)", poissonVar = YES
  //	  ),
  //	  flatPriorPPC.copy(
  //		label = "Weak (Poisson)", priorWeight = weakPrior.priorWeight, poissonVar = YES
  //	  ),
  //	  flatPriorPPC.copy(
  //		label = "Strong (Poisson)", priorWeight = strongPrior.priorWeight, poissonVar = YES
  //	  ),
  //	)
  //  )
  //  exps += exps.last().copy(
  //	name = "S4.C.3", series = listOf(
  //	  flatPriorPPC.copy(
  //		label = "Flat (Fake1 Poisson)", poissonVar = FAKE1
  //	  ), flatPriorPPC.copy(
  //		label = "Weak (Fake1 Poisson)", priorWeight = weakPrior.priorWeight, poissonVar = FAKE1
  //	  ), flatPriorPPC.copy(
  //		label = "Strong (Fake1 Poisson)", priorWeight = strongPrior.priorWeight, poissonVar = FAKE1
  //	  )
  //	)
  //  )
  //  exps += exps.last().copy(
  //	name = "S4.C.4", series = listOf(
  //	  flatPriorPPC.copy(
  //		label = "Flat (Fake10 Poisson)", poissonVar = FAKE10
  //	  ), flatPriorPPC.copy(
  //		label = "Weak (Fake10 Poisson)", priorWeight = weakPrior.priorWeight, poissonVar = FAKE10
  //	  ), flatPriorPPC.copy(
  //		label = "Strong (Fake10 Poisson)", priorWeight = strongPrior.priorWeight, poissonVar = FAKE10
  //	  )
  //	)
  //  )
  //
  //  exps += exps.last().copy(
  //	name = "S4.D", ylabel = "Baseline Activity", xlabel = "Preferred Orientation (Degrees)",
  //	title = "Bayesian Priors: Oblique effect is diminished in Autism (Pop. Gain)", xOp = stimOrientationXOpExample.xOp,
  //	series = listOf(
  //	  flatPriorPPCbase.copy(
  //		label = "Baseline Activity", priorWeight = strongPrior.priorWeight
  //	  )
  //	)
  //  )
  //
  //  fun CCWSeries(
  //	relThetaMid: Float, ccwTrials: Int
  //  ) = flatPriorPPC.copy(
  //	label = "$relThetaMid°",
  //	priorWeight = veryStrongPrior,
  //	stimCfg = priorSeriesStimCfg(relThetaMid, plusX = true),
  //	yExtractCustom = {
  //	  val substep = 1.0
  //	  (1..ccwTrials).map {
  //		val probs = (relThetaMid - (substep*9)..relThetaMid + substep*10 step substep).mapIndexed { _, theta ->
  //		  (decode(
  //			ftStim = seriesStim.copy(f = seriesStim.f.copy(tDegrees = theta)),
  //			trialStim = seriesStim,
  //		  ) to if (theta > relThetaMid) 1.0 else 0.0)
  //		}
  //		(probs.maxByOrNull { it.first }!!.second)
  //	  }.mean()
  //	},
  //	line = true, /*while im waiting for fixes*/
  //	markers = true,
  //	fit = Gaussian,
  //  )
  //
  //  exps += baseExp.copy(
  //	name = "S5.1",
  //	xlabel = "Relative Orientation (Degrees)",
  //	ylabel = "Population CCW Response",
  //	title = "Psychometric Simulation of the Oblique Effect (Deterministic)",
  //	xMin = -10.0,
  //	xMax = 10.0,
  //	xStep = 1.0,
  //	xOp = {},
  //	series = listOf(
  //	  CCWSeries(relThetaMid = 90.0f, ccwTrials = 1), CCWSeries(relThetaMid = 45.0f, ccwTrials = 1)
  //	),
  //	normToMinMax = false,
  //	autoY = true,
  //  )
  //
  //  exps += exps.last().copy(
  //	name = "S5.2", title = exps.last().title.replace("Deterministic", "Poisson"), series = listOf(
  //	  CCWSeries(relThetaMid = 90.0f, ccwTrials = 50).copy(poissonVar = YES),
  //	  CCWSeries(relThetaMid = 45.0f, ccwTrials = 50).copy(poissonVar = YES)
  //	)
  //  )
  //  exps += exps.last().copy(name = "S5.3", title = exps.last().title.replace("Poisson", "Fake1 Poisson"),
  //	series = exps[exps.size - 2].series.map {
  //	  (it as matt.v1.figmodels.matt.v1.figmodels.SeriesCfg).copy(
  //		poissonVar = FAKE1
  //	  )
  //	})
  //  exps += exps.last().copy(name = "S5.4", title = exps.last().title.replace("Fake1 Poisson", "Fake5 Poisson"),
  //	series = exps.last().series.map {
  //	  (it as matt.v1.figmodels.matt.v1.figmodels.SeriesCfg).copy(
  //		poissonVar = FAKE5
  //	  )
  //	})
  //  exps += exps.last().copy(name = "S5.5", title = exps.last().title.replace("Fake5 Poisson", "Fake10 Poisson"),
  //	series = exps.last().series.map {
  //	  (it as matt.v1.figmodels.matt.v1.figmodels.SeriesCfg).copy(
  //		poissonVar = FAKE10
  //	  )
  //	})
  //
  //  val p0 = matt.v1.figmodels.matt.v1.figmodels.SeriesCfg(
  //	label = "P=0",
  //	yExtractCustom = { PPCUnit(ft = x, ri = x.assertRound())().toDouble() },
  //  )
  //  exps += baseExp.copy(
  //	name = "Ideal Poisson", title = "Ideal Neuronal Responses for PPCs", xlabel = "f(Θ):Mean Neural Response",
  //	ylabel = "PPC Unit Probability (informativeness)", autoY = true, normToMinMax = false, category = OTHER, xMin = 0.0,
  //	xMax = 5.0, xStep = 0.025, xOp = {}, series = listOf(
  //	  p0, *(1..4).map { i ->
  //		p0.copy(label = "P=$i", yExtractCustom = { PPCUnit(ft = x, ri = (x + i).assertRound())().toDouble() })
  //	  }.toTypedArray()
  //	)
  //  )
  //
  //  val h = 0.01
  //  var lastPopR: PopulationResponse? = null
  //  val preferedCellR = matt.v1.figmodels.matt.v1.figmodels.SeriesCfg(label = "Preferred Cell R", yExtractCustom = {
  //	val xi = itr.indexOf(x)
  //	lastPopR = MaybePreDNPopR(
  //	  stim = seriesStim, attention = outer.useAttentionMask, pop = outer.pop,
  //	  uniformW = outer.uniformW, /*SHOULD BE COMBINED WITH SIGMAPOOLING*/
  //	  rawInput = outer.rawInput, ti = xi.toApint(), h = h.toApfloat(), lastPopR = lastPopR,
  //	  sigmaPooling = outer.pop.cfg.activityConfig.baseSigmaPooling, /*SHOULD BE COMBINED WITH uniformW*/
  //	  divNorm = outer.pop.cfg.activityConfig.baseDivNorm
  //	)()
  //	val y = lastPopR!!.m[outer.pop.centralCell]!!.R
  //	y
  //  })
  //
  //  val preferedCellG = matt.v1.figmodels.matt.v1.figmodels.SeriesCfg(label = "Preferred Cell G", yExtractCustom = {
  //	val y = lastPopR!!.m[outer.pop.centralCell]!!.G_S ?: 0.0
  //	if (x == itr.last()) lastPopR = null
  //	y
  //  })
  //
  //  fun SFExciteSeries(sf: Double) = matt.v1.figmodels.matt.v1.figmodels.SeriesCfg(label = "SF:${sf}", yExtractCustom = {
  //	val cell = outer.pop.centralCellWithSF(sf.toApfloat())
  //	val y = lastPopR!!.m[cell]!!.R
  //	if (x == itr.last()) lastPopR = null
  //	y
  //  })
  //
  //  val louieBaseExp = Experiment(
  //	name = "Dynamic", title = "Preferred Cell Response Over Time", xlabel = "Time", ylabel = "Preferred Cell Response",
  //	autoY = true, fig = fig, statusLabel = statusLabel, xOp = {
  //	  ti = itr.indexOf(x).toApint()
  //	  this.h = outer.xStep!!.toApfloat()
  //	}, xStep = h, series = listOf(preferedCellR, preferedCellG), category = LOUIE, xMax = 10.0,
  //	uniformW = 1.0.toApfloat(), rawInput = 30.0.toApfloat(), popCfg = popLouie
  //  )
  //  exps += louieBaseExp
  //  exps += louieBaseExp.copy(
  //	name = louieBaseExp.name + " (normToMaxes)", normToMinMax = true
  //  )
  //  exps += louieBaseExp.copy(
  //	name = louieBaseExp.name + " (5 cells)", popCfg = popLouieMoreCells
  //  )
  //  exps += exps.last().copy(
  //	name = "+ 180*5 cells", popCfg = popLouieFullThetaCells
  //  )
  //  exps += exps.last().copy(
  //	name = "+ excitation only",
  //	series = listOf(preferedCellR),
  //  )
  //  exps += exps.last().copy(
  //	name = "+ real stimuli, weight kernel", rawInput = null, uniformW = null
  //  )
  //  exps += exps.last().copy(
  //	name = "+ larger cell spacing, but with 5 degree theta step to reduce #cells",
  //	popCfg = latestPop,
  //  )
  //
  //
  //    exps += exps.last().copy(
  //	  name = "SF Peak Latency",
  //	  popCfg = latestPop.copy(
  //		proportionalCellSigmas = true,
  //		cellSFmin = 2.0.toApfloat(),
  //		cellSFmax = 10.0.toApfloat(),
  //		cellSFstep = 2.0.toApfloat(),
  //		sampleStep = 0.5
  //	  ),
  //	  series = listOf(exps.last().series[0]) + (2..10 step 2).map {
  //		SFExciteSeries(it.toDouble())
  //	  } /*pointer issue?*/
  //  )
  //
  //
  //
  //
  //
  //


}

