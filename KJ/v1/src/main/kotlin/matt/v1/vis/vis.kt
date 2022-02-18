package matt.v1.vis

import javafx.beans.property.SimpleObjectProperty
import matt.hurricanefx.eye.lib.onChange
import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.file.get
import matt.kjlib.jmath.floorInt
import matt.klib.dmap.withStoringDefault
import matt.v1.gui.FilteredImageVisualizer
import matt.v1.gui.GeneratedImageVisualizer
import matt.v1.lab.petri.PopulationConfig
import matt.v1.lab.petri.pop2D
import matt.v1.model.FieldGenerator
import matt.v1.model.Phase.COS
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import matt.v1.salience.FeatureDef
import matt.v1.salience.FeatureType
import matt.v1.salience.Salience
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.io.File

class RosenbergVisualizer(
  popCfg: PopulationConfig,
): GeneratedImageVisualizer(
  responsive = true,
  imageHW = popCfg.fieldHW.floorInt().apply {
	println("DEBUG:${this}")
  },
  imgScale = 0.05,
) {

  fun addStim(stim: FieldGenerator) {
	theStim = stim
	visualize()
  }


  private var gen by CfgObjProp<FieldGenerator?>(
	popCfg.baseStim.copy(f = popCfg.baseStim.f.copy(X0 = 0.0)),
	popCfg.baseSimpleSinCell.copy(f = popCfg.baseSimpleSinCell.f.copy(X0 = 0.0)),
	popCfg.baseSimpleSinCell.copy(f = popCfg.baseSimpleSinCell.f.copy(X0 = 0.0), phase = COS),
	null
  )

  @Suppress("PrivatePropertyName")
  private val SF by CfgDoubleProp(0.01 to 10, default = (gen as Stimulus).SF)
  private val theta by CfgDoubleProp(pop2D.prefThetaMin to pop2D.prefThetaMax, default = (gen as Stimulus).tDegrees)
  private val sigmaMult by CfgDoubleProp(0.01 to 10, default = 1.0)
  private val sampleStep by CfgDoubleProp(0.1 to 20, default = (gen as Stimulus).field.sampleStep)
  private val gaussian by CfgBoolProp(default = (gen as Stimulus).gaussianEnveloped)
  override fun update() {
	val g = gen
	if (g is Stimulus) {
	  theStim = g.copy(
		SF = SF,
		f = g.f.copy(tDegrees = theta, field = g.f.field.copy(sampleStep = sampleStep)),
		s = g.s*sigmaMult,
		gaussianEnveloped = gaussian,
	  )
	} else if (g is SimpleCell) {
	  theStim = g.copy(
		SF = SF,
		f = g.f.copy(tDegrees = theta, field = g.f.field.copy(sampleStep = sampleStep)),
		sx = g.sx*sigmaMult,
		sy = g.sy*sigmaMult,
		gaussianEnveloped = gaussian
	  )
	}
	/*(theStim as Stimulus).debug = true
	println("DEBUG1=${theStim!!.pix(Point(x = 0.0, y = 0.0))}")
	println("DEBUG2=${theStim!!.pix(Point(x = 5.0, y = 0.0))}")
	println("DEBUG3=${theStim!!.pix(Point(x = 0.0, y = 5.0))}")
	println("DEBUG4=${theStim!!.pix(Point(x = 5.0, y = 5.0))}")
	(theStim as Stimulus).debug = false*/
  }


  /*x y here are pixel coordinates of displayed image, ints between 1 and 100 for now*//*
  override fun draw(x: Int, y: Int) = run {
	val rg = theStim?.getVisSample(x/HWd, y/HWd) ?: 0.0
	val b = theCell?.getVisSample(x/HWd, y/HWd) ?: 0.0
	Color.color(rg, rg, b)
  }!!*/

  init {
	update()
	visualize()
  }
}

class IttiKochVisualizer: FilteredImageVisualizer(
  responsive = true,
  imageHW = 513 /*works for the 8 pyramid downscales used in itti koch*/,
  imgScale = 1.5,
  staticImages = listOf(ittiKochInput)
) {

  companion object {
	val dogFolder = DATA_FOLDER["kcomp/google_dogs"]
	val ittiKochInput = SimpleObjectProperty(dogFolder.listFiles()!!.first())
  }

  private val featureCache = mutableMapOf<String, Map<FeatureDef, NDArray<Int, D2>>>().withStoringDefault {
	Salience().computeFeatures(preprocessImage(File(it))).apply {
	  println("computed $size features")
	}
  }

  private val type by CfgObjProp(*FeatureType.values())
  private val center by CfgIntProp(2..4, default = 2)
  private val surround by CfgIntProp(3..4, default = 3)


  override fun draw(input: File) = run {
	val features = featureCache[input.absolutePath]
	features[FeatureDef(center = center, surround = surround, type = type)]!!
  }

  init {
	ittiKochInput.apply {
	  onChange {
		visualize()
	  }
	}
	visualize()
  }
}