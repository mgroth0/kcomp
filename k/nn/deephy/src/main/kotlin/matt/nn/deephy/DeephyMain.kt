@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.scene.control.ChoiceBox
import javafx.stage.DirectoryChooser
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.MFile
import matt.file.mFile
import matt.file.toMFile
import matt.fx.graphics.lang.actionbutton
import matt.fx.graphics.layout.hbox
import matt.fx.graphics.layout.vbox
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.IProp
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.objectBinding
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.tornadofx.nodes.clear
import matt.hurricanefx.wrapper.VBoxWrapper
import java.util.prefs.Preferences
import kotlin.reflect.KProperty


@Serializable
class Top(
  val layer_ID: Int,
  val layer_Name: String,
  val num_Neurons: Int,
  val top_100: Array<Array<Int>>
)

@Serializable
class Image(
  val `image ID`: String,
  val type: String,
  val category: String,
  val file: Map<String, Array<Array<Int>>>
)

val prefs: Preferences = Preferences.userRoot().node("sinhalab.deephy")

class Pref(val defaultValue: String? = null) {

  operator fun provideDelegate(
	thisRef: Any?,
	prop: KProperty<*>
  ): Pref {
	return this
  }

  operator fun getValue(
	thisRef: Any?,
	property: KProperty<*>
  ) = prefs.get(property.name, defaultValue)


  operator fun setValue(
	thisRef: Any?,
	property: KProperty<*>,
	value: String?
  ) {
	if (value == null) {
	  prefs.remove(property.name)
	} else prefs.put(property.name, value)
  }
}

fun main() = GuiApp(decorated = true) {


  var dataFolder by Pref()
  val dataFolderProperty = Prop<MFile>()
  dataFolder?.let { dataFolderProperty.value = mFile(it) }
  dataFolderProperty.onChange {
	dataFolder = it?.abspath
  }

  val dataFile = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test.cbor")
  }
  val dataFileTop = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test_top.cbor")
  }
  val statusProp = SProp("")

  val resultBox = VBoxWrapper()

  rootVbox {
	label(dataFolderProperty.stringBinding {
	  "data folder: $it"
	})
	actionbutton("choose data folder") {
	  val f = DirectoryChooser().apply {
		title = "choose data folder"
	  }.showDialog(stage)

	  if (f != null) {
		dataFolderProperty.value = f.toMFile()
	  }
	}
	actionbutton("load data") {
	  if (dataFileTop.value == null) {
		statusProp.value = "please select data folder"
	  } else if (dataFileTop.value!!.doesNotExist) {
		statusProp.value = "${dataFileTop.value} does not exist"
	  } else if (dataFile.value!!.doesNotExist) {
		statusProp.value = "${dataFile.value} does not exist"
	  } else {
		val top = Cbor.decodeFromByteArray<Top>(dataFileTop.value!!.readBytes())
		/*val data = Cbor.decodeFromByteArray<Array<Image>>(dataFileTop.value!!.readBytes())*/
		resultBox.clear()
		resultBox.apply {
		  label("Layer ID: ${top.layer_ID}")
		  label("Layer Name: ${top.layer_Name}")
		  label("Num Neurons: ${top.num_Neurons}")
		  val unitIndexProp = IProp()
		  var cb: ChoiceBox<Int>? = null
		  hbox {
			label("choose neuron: ")
			cb = choicebox(values = (0..top.num_Neurons).toList().toObservable())
		  }
		  vbox {
			cb!!.valueProperty().onChange {
			  clear()
			  if (it != null) {
				label(
				  "top images of unit ${it}: " + top.top_100[it].joinToString(
					prefix = "[", postfix = "]", separator = ","
				  )
				) {
				  isWrapText = true
				}
			  }
			}
		  }
		}
		statusProp.value = "got data"
	  }
	}
	label(statusProp)
	+resultBox
  }

}.start()