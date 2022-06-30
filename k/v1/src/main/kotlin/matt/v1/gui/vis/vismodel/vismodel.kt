
package matt.v1.gui.vis.vismodel

import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.ContentDisplay.TOP
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import matt.caching.with
import matt.fx.graphics.async.runLaterReturn
import matt.fx.graphics.clip.dragsSnapshot
import matt.fx.graphics.layout.hbox
import matt.gui.proto.ScaledCanvas
import matt.hurricanefx.exactHeight
import matt.hurricanefx.exactHeightProperty
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.lib.onNonNullChange
import matt.hurricanefx.eye.prop.plus
import matt.hurricanefx.stage
import matt.hurricanefx.tornadofx.control.imageview
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.clear
import matt.kjlib.image.resize
import matt.kjlib.image.toSquare
import matt.file.MFile
import matt.stream.forEachNested
import matt.v1.cfg.GuiConfigurable
import matt.v1.model.FieldGenerator
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.operations.forEachIndexed
import java.awt.image.BufferedImage
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO
import kotlin.concurrent.thread

abstract class ImageVisualizer(
  responsive: Boolean = false,
  protected val imageHW: Int,
  val imgScale: Double,
): GuiConfigurable(responsive = responsive) {
  protected val HWd = imageHW.toDouble()

  val node = VBox()
  protected val imageBox = node.hbox {
	alignment = CENTER_LEFT
	prefWidthProperty().bind(node.widthProperty())
  }


  open fun update() {}
  abstract fun visualize()
  override fun onConfigChanged() {
	update()
	visualize()
  }

  protected var canv: ScaledCanvas? = null

  open fun left() {}

  init {
	node.alignment = TOP_CENTER

	left()
	imageBox.label("configurable") {

	  contentDisplay = TOP
	  graphic = ScaledCanvas(hw = imageHW, scale = imgScale).apply {
		/*green()*/
		dragsSnapshot()
		canv = this
	  }
	  this.prefHeightProperty().bind(canv!!.heightProperty())
	  imageBox.exactHeightProperty().bind(this.heightProperty())
	}
	Platform.runLater { /*must runlater to get child props*/
	  val cfgPane = configPane().apply {
		exactHeight = 150.0
	  }

	  if (node.scene != null) {
		cfgPane.prefWrapLengthProperty().bind(node.stage!!.widthProperty())
	  }
	  node.sceneProperty().onNonNullChange {
		cfgPane.prefWrapLengthProperty().bind(node.stage!!.widthProperty())
	  }

	  node.add(cfgPane)
	  node.minHeightProperty().bind(imageBox.heightProperty() + cfgPane.heightProperty() /*+ 350.0*/)
	}
  }
}

abstract class GeneratedImageVisualizer(
  responsive: Boolean = false,
  imageHW: Int,
  imgScale: Double,
): ImageVisualizer(responsive, imageHW, imgScale) {

  var theStim: FieldGenerator? = null
  override fun visualize() {
	canv!!.drawObj(theStim!!)
  }/*=

	(0..imageHW)
	  .forEachNested { x, y ->
		canv!![x, y] = draw(x, y)
	  }*/

  /*  abstract fun draw(x: Int, y: Int): matt.klib.css.Color*/


  val theBox = imageBox.hbox {
	alignment = CENTER_LEFT
	prefHeightProperty().bind(canv!!.heightProperty())
  }

  val sem = Semaphore(1)
  fun setGens(vararg gens: Pair<String, FieldGenerator>) {
	thread {
	  sem.with {
		if (sem.queueLength == 0) { /*not sure if queueLength is reliable*/
		  runLaterReturn {
			theBox.apply {
			  clear()
			  gens.forEach { g ->
				label(g.first, graphic = ScaledCanvas(hw = imageHW, scale = imgScale).apply {
				  drawObj(g.second)
				  dragsSnapshot()
				}
				) {
				  contentDisplay = TOP
				}
			  }
			}
		  }
		}
	  }
	}
  }

  fun ScaledCanvas.drawObj(g: FieldGenerator) {
	(0..imageHW)
	  .forEachNested { x, y ->
		val v = g.getVisSample(x = x/HWd, y = y/HWd, relWithSign = true) ?: 0.0
		var rg = 0.0
		var b = 0.0
		if (v > 0.0) {
		  b = v
		} else if (v < 0.0) {
		  rg = -v
		}
		this[x, y] = Color.color(rg, rg, b)
	  }
  }


}

abstract class FilteredImageVisualizer(
  responsive: Boolean = false,
  imageHW: Int,
  imgScale: Double,
  private val staticImages: List<ObjectProperty<MFile>> = listOf()
): ImageVisualizer(responsive, imageHW, imgScale) {

  protected fun preprocessImage(file: MFile): BufferedImage {
	return ImageIO.read(file).toSquare().resize(imageHW, imageHW)
  }

  protected fun preprocessImageFX(file: MFile): Image {
	return SwingFXUtils.toFXImage(preprocessImage(file), null)
  }

  override fun visualize() {
	draw(staticImages[0].value).forEachIndexed { i, d ->
	  canv!![i[0], i[1]] = Color.rgb(d, d, d)
	}
  }

  abstract fun draw(input: MFile): MultiArray<Int, D2>

  override fun left(): Unit {
	staticImages.forEach {
	  imageBox.imageview(preprocessImageFX(it.value)) {
		Platform.runLater {
		  fitWidthProperty().bind(canv!!.widthProperty())
		  fitHeightProperty().bind(canv!!.heightProperty())
		}
		it.onChange {
		  image = preprocessImageFX(it!!)
		}
	  }
	}


  }

}