package matt.v1.vis

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.paint.Color
import matt.hurricanefx.eye.lib.onChange
import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.file.get
import matt.klib.dmap.withStoringDefault
import matt.v1.gui.FilteredImageVisualizer
import matt.v1.gui.GeneratedImageVisualizer
import matt.v1.lab.petri.PopulationConfig
import matt.v1.lab.petri.pop2D
import matt.v1.model.SimpleCell
import matt.v1.model.SimpleCell.Phase.COS
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
  imageHW = 100,
  imgScale = 3.0,
) {
  private var theStim: Stimulus? = null
  private var theCell: SimpleCell? = null
  private var stimulus by CfgObjProp(popCfg.baseStim.copy(f = popCfg.baseStim.f.copy(X0 = 0.0)), null)
  private var cell by CfgObjProp(
	null,
	popCfg.baseSimpleSinCell.copy(f = popCfg.baseSimpleSinCell.f.copy(X0 = 0.0)),
	popCfg.baseSimpleSinCell.copy(f = popCfg.baseSimpleSinCell.f.copy(X0 = 0.0), phase = COS),
  )

  @Suppress("PrivatePropertyName")
  private val SF by CfgDoubleProp(0.01 to 1)
  private val theta by CfgDoubleProp(pop2D.prefThetaMin to pop2D.prefThetaMax)
  private val sigmaMult by CfgDoubleProp(0.01 to 10)
  private val gaussian by CfgBoolProp(true)
  override fun update() {
	theStim = stimulus?.copy(
	  SF = SF,
	  f = stimulus!!.f.copy(t = theta),
	  s = stimulus!!.s*sigmaMult,
	  gaussianEnveloped = gaussian
	)
	theCell = cell?.copy(
	  SF = SF,
	  f = cell!!.f.copy(t = theta),
	  sx = cell!!.sx*sigmaMult,
	  sy = cell!!.sy*sigmaMult,
	  gaussianEnveloped = gaussian
	)
  }

  override fun draw(x: Int, y: Int) = run {
	val rg = theStim?.getVisSample(x/HWd, y/HWd) ?: 0.0
	val b = theCell?.getVisSample(x/HWd, y/HWd) ?: 0.0
	Color.color(rg, rg, b)
  }!!

  init {
	update()
	visualize()
  }
}

/*1*2^8+1 = 257*/
/*2*2^8+1 = 513*/
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