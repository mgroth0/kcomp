package matt.v1.gui

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.layout.VBox
import matt.hurricanefx.eye.prop.objectBinding
import matt.hurricanefx.tornadofx.fx.attachTo
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.visibleAndManagedProp
import matt.hurricanefx.wrapper.wrapped
import matt.v1.gui.GuiMode.ITTI_KOCH
import matt.v1.gui.GuiMode.ROSENBERG
import matt.v1.gui.expbox.expBox
import matt.v1.gui.status.StatusLabel
import matt.v1.gui.vis.VisualizerPane


enum class GuiMode { ROSENBERG, ITTI_KOCH }

class V1Gui(startup: GuiMode, remoteStatus: StatusLabel?): VBox() {

  private val modeProp = SimpleObjectProperty(startup)

  init {


	alignment = TOP_CENTER/*val figHeightProp = DProp(500.0)*/

	val visPane = VisualizerPane(startup)
	wrapped().add(visPane.wrapped())
	val ittKochTab = visPane.tabs[1]
	modeProp.bind(ittKochTab.selectedProperty().objectBinding {
	  if (it!!) ITTI_KOCH else ROSENBERG
	})
	expBox {
	  visibleAndManagedProp().bind(modeProp.isEqualTo(ROSENBERG))
	}
	remoteStatus?.attachTo(this.wrapped())
  }
}