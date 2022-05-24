package matt.v1.gui.vis

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ComboBox
import javafx.scene.control.TabPane
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.StringConverter
import matt.gui.resize.DragResizer
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.lazyTab
import matt.kbuild.DATA_FOLDER
//import matt.kjlib.commons.DATA_FOLDER
import matt.kjlib.file.get
import matt.kjlib.jmath.floorInt
import matt.kjlib.lang.NEVER
import matt.klib.dmap.withStoringDefault
import matt.v1.gui.GuiMode
import matt.v1.gui.GuiMode.ITTI_KOCH
import matt.v1.gui.vis.vismodel.FilteredImageVisualizer
import matt.v1.gui.vis.vismodel.GeneratedImageVisualizer
import matt.v1.low.PhaseType.COS
import matt.v1.model.FieldGenerator
import matt.v1.model.Orientation
import matt.v1.model.SimpleCell
import matt.v1.model.Stimulus
import matt.v1.model.combined.CombinedConfig
import matt.v1.salience.FeatureDef
import matt.v1.salience.FeatureType
import matt.v1.salience.Salience
import matt.v1.visualizer
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.io.File

class VisualizerPane(startup: GuiMode): TabPane() {
  init{
	tabs += lazyTab("Rosenberg") { visualizer.node }
	tabs += lazyTab("Itti Koch") {
	  this.isDisable = true /*it throws an error and I dont have time for this right now*/
	  VBox(
		ComboBox(
		  IttiKochVisualizer.dogFolder.listFiles()!!.sorted().toObservable()
		).apply {
		  valueProperty().bindBidirectional(
			IttiKochVisualizer.ittiKochInput
		  )		/*simpleCellFactory { it.name to null }*/
		  this.converter = object: StringConverter<File>() {
			override fun toString(`object`: File?): String {
			  return `object`!!.name
			}

			override fun fromString(string: String?): File {
			  NEVER
			}

		  }
		}, IttiKochVisualizer().node
	  ).apply { alignment = CENTER }
	}
	when (startup) {
	  GuiMode.ROSENBERG -> selectionModel.select(tabs.first())
	  ITTI_KOCH         -> selectionModel.select(tabs.last())
	}

	/*DragResizer.makeResizable(this) {
	  figHeightProp.value = kotlin.math.max(kotlin.math.min(figHeightProp.value - it, 600.0), 300.0)
	}*/
	border = Border(
	  BorderStroke(
		null, null, Color.GRAY, null, null, null, BorderStrokeStyle.SOLID, null, null,
		BorderWidths(0.0, 0.0, DragResizer.RESIZE_MARGIN.toDouble(), 0.0), null
	  )
	)

  }
}

class RosenbergVisualizer(
  popCfg: CombinedConfig,
): GeneratedImageVisualizer(
  responsive = true,
  imageHW = popCfg.fieldConfig.fieldHW.floorInt().apply {
	println("DEBUG:${this}")
  },
  imgScale = 0.5,
) {

  fun addStim(stim: FieldGenerator) {
	theStim = stim
	visualize()
  }


  private var gen by CfgObjProp<FieldGenerator?>(
	popCfg.baseStim.copy(
	  fieldLoc = popCfg.baseStim.fieldLoc.clone(newX = 0)
	),
	popCfg.baseSimpleSinCell.copy(
	  fieldLoc = popCfg.baseSimpleSinCell.fieldLoc.clone(newX = 0)
	),
	popCfg.baseSimpleSinCell.toCellWithPhase(COS).copy(
	  fieldLoc = popCfg.baseSimpleSinCell.fieldLoc.clone(newX = 0)
	),
	null
  )

  @Suppress("PrivatePropertyName")
  private val SF by CfgDoubleProp(0.01 to 10, default = (gen as Stimulus).SF)
  private val theta by CfgDoubleProp(popCfg.populationConfig.prefThetaMin to popCfg.populationConfig.prefThetaMax, default = (gen as Stimulus).o.tDegrees)
  private val sigmaMult by CfgDoubleProp(0.01 to 10, default = 1.0)
  private val sampleStep by CfgDoubleProp(0.1 to 20, default = (gen as Stimulus).fieldCfg.sampleStep)
  private val gaussian by CfgBoolProp(default = (gen as Stimulus).gaussianEnveloped)
  override fun update() {
	val g = gen
	if (g is Stimulus) {
	  theStim = g.copy(
		SF = SF,
		o = Orientation(theta),
		fieldCfg = g.fieldCfg.copy(sampleStep=sampleStep),
		s = g.s*sigmaMult,
		gaussianEnveloped = gaussian,
	  )
	} else if (g is SimpleCell<*>) {
	  theStim = g.copy(
		SF = SF,
		o = Orientation(theta),
		fieldCfg = g.fieldCfg.copy(sampleStep=sampleStep),
		sx = g.sx*sigmaMult,
		sy = g.sy*sigmaMult,
		gaussianEnveloped = gaussian
	  )
	}
	/*(theStim as Stimulus).debug = true
	println("DEBUG1=${theStim!!.pix(matt.v1.mathexport.point.matt.kjlib.jmath.point.Point(x = 0.0, y = 0.0))}")
	println("DEBUG2=${theStim!!.pix(matt.v1.mathexport.point.matt.kjlib.jmath.point.Point(x = 5.0, y = 0.0))}")
	println("DEBUG3=${theStim!!.pix(matt.v1.mathexport.point.matt.kjlib.jmath.point.Point(x = 0.0, y = 5.0))}")
	println("DEBUG4=${theStim!!.pix(matt.v1.mathexport.point.matt.kjlib.jmath.point.Point(x = 5.0, y = 5.0))}")
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