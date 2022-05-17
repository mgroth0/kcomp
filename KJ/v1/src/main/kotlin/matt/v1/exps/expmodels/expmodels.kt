package matt.v1.exps.expmodels

import kotlinx.coroutines.flow.flow
import matt.kjlib.async.emitAll
import matt.kjlib.stream.mutableListsOf
import matt.v1.activity.MaybePreDNPopR
import matt.v1.activity.Stimulation
import matt.v1.exps.expmodels.ExpCategory.ROSENBERG
import matt.v1.figmodels.AxisConfig
import matt.v1.figmodels.SeriesCfgV2
import matt.v1.gui.fig.update.SeriesUpdate.Companion.replaceNextSeries
import matt.v1.gui.fig.update.StatusUpdate
import matt.v1.jsonpoint.JsonPoint
import matt.v1.lab.Experiment
import matt.v1.lab.petri.Population
import matt.v1.model.Stimulus
import matt.v1.model.activity.ARI_ASD_DN_CFG
import matt.v1.model.combined.ARI_BASE_CFG
import matt.v1.scaling.PerformanceMode.DEBUG
import matt.v1.scaling.copyWith

enum class ExpCategory { ROSENBERG, LOUIE, OTHER; }

fun varyingStimExp(
  expID: Pair<Int, Char>,
  title: String,
  xAxisConfig: AxisConfig,
  yAxisConfig: AxisConfig,
  inputRange: Iterable<Double>,
  xStimOp: Stimulus.(Double)->Stimulus,
) = Experiment(
  name = "${expID.first}.${expID.second}",
  title = title,
  xAxisConfig = xAxisConfig,
  yAxisConfig = yAxisConfig,
  series = listOf(
	SeriesCfgV2(label = "- D.N. (R)"),
	SeriesCfgV2(label = "+ TD-D.N. (R)"),
	SeriesCfgV2(label = "+ TD-D.N. (S)"),
	SeriesCfgV2(label = "+ ASD-D.N. (R)"),
	SeriesCfgV2(label = "+ ASD-D.N. (S)"),
  ),
  category = ROSENBERG,
  v2 = flow {

	val allSeries = mutableListsOf<JsonPoint>(5)
	val (s1, s2, s3, s4, s5) = allSeries

	val cfg = ARI_BASE_CFG.copyWith(DEBUG)
	val pop = Population(cfg)
	val xRange = inputRange.toList()
	xRange.forEachIndexed { xIndex, x ->

	  emit(StatusUpdate.new("x", xIndex + 1, xRange.size))

	  val stim = pop.cfg.perfectStimFor(pop.centralCell).xStimOp(x)

	  s1 += JsonPoint(
		x = x,
		y = Stimulation(
		  activityConfig = cfg.activityConfig,
		  cell = pop.centralCell,
		  input = stim,
		)().R
	  )

	  val popR = MaybePreDNPopR(
		activityConfig = cfg.activityConfig,
		input = stim,
		pop = pop,
	  ).findOrCompute {
		emit(StatusUpdate.new("stimulated cells", it, pop.complexCells.size))
	  }

	  val tdDNStim = Stimulation(
		activityConfig = cfg.activityConfig,
		cell = pop.centralCell,
		input = stim,
		popR = popR
	  )

	  s2 += JsonPoint(x = x, y = tdDNStim().R)
	  s3 += JsonPoint(x = x, y = tdDNStim().G_S!!)

	  val asdDNStim = tdDNStim.copy(
		activityConfig = tdDNStim.activityConfig.copy(
		  cfgDN = ARI_ASD_DN_CFG
		)
	  )

	  s4 += JsonPoint(x = x, y = asdDNStim().R)
	  s5 += JsonPoint(x = x, y = asdDNStim().G_S!!)

	  emitAll(allSeries.map { points ->
		replaceNextSeries(yAxisConfig.postProcessOp(xAxisConfig.postProcessOp(points)))
	  })
	}
  }
)

