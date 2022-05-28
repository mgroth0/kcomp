package matt.v1.cfg

import javafx.application.Platform
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.FlowPane
import javafx.util.StringConverter
import matt.gui.lang.actionbutton
import matt.hurricanefx.exactWidth
import matt.hurricanefx.exactWidthProperty
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.times
import matt.hurricanefx.tornadofx.control.checkbox
import matt.hurricanefx.tornadofx.control.label
import matt.hurricanefx.tornadofx.control.slider
import matt.hurricanefx.tornadofx.item.combobox
import matt.hurricanefx.tornadofx.layout.hbox
import matt.hurricanefx.tornadofx.nodes.add
import matt.kjlib.jmath.sigFigs
import matt.kjlib.str.truncateWithElipses
import matt.klib.lang.NEVER
import matt.klib.tfx.isDouble
import matt.klib.tfx.isFloat
import matt.klib.tfx.isInt
import kotlin.math.floor
import kotlin.reflect.KProperty

abstract class GuiConfigurable(
  val responsive: Boolean
) {

  abstract fun onConfigChanged()

  val props = mutableListOf<CfgProp<*>>()

  abstract inner class CfgProp<T> {
	abstract var value: Any?
	abstract val default: T
	lateinit var name: String
	operator fun provideDelegate(
	  thisRef: GuiConfigurable,
	  prop: KProperty<*>
	): CfgProp<T> {
	  props += this
	  name = prop.name
	  return this
	}

	operator fun getValue(
	  thisRef: GuiConfigurable,
	  property: KProperty<*>
	): T {
	  return value as T
	}

	operator fun setValue(
	  thisRef: GuiConfigurable,
	  property: KProperty<*>,
	  newValue: T
	) {
	  value = newValue
	}
  }

  inner class CfgObjProp<T>(vararg val values: T): CfgProp<T>() {
	override val default = values.first()
	override var value: Any? = default
  }

  sealed interface SliderProp<N: Number> {
	val min: N
	val max: N
  }

  inner class CfgIntProp(val range: IntRange, default: Int? = null): CfgProp<Int>(), SliderProp<Int> {
	override val default = default ?: range.first.let {
	  val mid = (range.last - range.first) + range.first
	  var v = it
	  while (v < mid) {
		v += range.step
	  }
	  v
	}
	override val min get() = range.first
	override val max get() = range.last
	override var value: Any? = default
  }

  inner class CfgFloatProp(val range: Pair<Number, Number>, default: Float? = null): CfgProp<Float>(),
																					 SliderProp<Float> {
	override val min get() = range.first.toFloat()
	override val max get() = range.second.toFloat()
	override val default =
	  default ?: (((range.second.toFloat() + range.first.toFloat())/2.0f) + range.first.toFloat())
	override var value: Any? = default
  }

  inner class CfgDoubleProp(val range: Pair<Number, Number>, default: Double? = null): CfgProp<Double>(),
																					   SliderProp<Double> {
	override val min get() = range.first.toDouble()
	override val max get() = range.second.toDouble()
	override val default =
	  default ?: (((range.second.toDouble() + range.first.toDouble())/2.0) + range.first.toDouble())
	override var value: Any? = default
  }

  inner class CfgBoolProp(override val default: Boolean): CfgProp<Boolean>() {
	override var value: Any? = default
  }

  fun configPane() = FlowPane().apply {
	this.orientation = VERTICAL
	val fp = this
	hgap = 10.0
	vgap = 10.0
	props.forEach { p ->
	  when (p) {
		is CfgObjProp<*> -> combobox(values = p.values.toList()) {
		  value = p.value
		  /*maxWidthProperty().bind(fp.widthProperty())*/
		  /*exactWidthProperty().bind(fp.widthProperty()*.4)*/
		  exactWidth = 400.0
		  promptText = p.name + "?"
		  valueProperty().onChange {
			println("obj changed: $it")
			p.value = it
			onConfigChanged()
		  }
		  converter = object: StringConverter<Any?>() {
			override fun toString(o: Any?): String {
			  return o.toString().truncateWithElipses(10)
			}

			override fun fromString(string: String?): Any? {
			  NEVER
			}

		  }
		}
		is CfgBoolProp   -> checkbox(p.name) {
		  isSelected = p.value as Boolean
		  exactWidthProperty().bind(fp.widthProperty()*.4)
		  selectedProperty().onChange {
			p.value = it
			onConfigChanged()
		  }
		}


		is SliderProp<*> -> hbox {





		  var changingFromSlider = false
		  var changingFromText = false

		  var theSlider: Slider? = null

		  actionbutton("reset") {
			theSlider!!.value = (p.default as Number).toDouble()
		  }

		  label(p.name + ":")

		  theSlider = when (p) {
			is CfgIntProp    -> slider(min = p.range.first, max = p.range.last, value = (p.value as Int).toDouble())
			is CfgDoubleProp -> slider(min = p.range.first, max = p.range.second, value = p.value as Double)
			is CfgFloatProp  -> slider(min = p.range.first, max = p.range.second, value = p.value as Float)
		  }



		  val tf = TextField(p.value.toString()).apply {
			prefWidth = 100.0
			textProperty().addListener { observable, oldValue, newValue ->
			  if (!changingFromSlider) {
				val b = when (p) {
				  is CfgIntProp    -> (newValue.isInt() && newValue.toInt() >= p.min && newValue.toInt() <= p.max)
				  is CfgDoubleProp -> (newValue.isDouble() && newValue.toDouble() >= p.min && newValue.toDouble() <= p.max)
				  is CfgFloatProp  -> (newValue.isFloat() && newValue.toFloat() >= p.min && newValue.toFloat() <= p.max)
				}
				if (b) {
				  /*p.value = newValue
				  onConfigChanged()*/
				  theSlider!!.value = newValue.toDouble()
				}
			  }
			}
		  }

		  exactWidthProperty().bind(fp.widthProperty()*.4)
		  theSlider!!.apply {
			isSnapToTicks = p is CfgIntProp
			isShowTickMarks = p is CfgIntProp
			isShowTickLabels = p is CfgIntProp
			majorTickUnit = 1.0
			minorTickCount = 0
			fun updateText() {
			  changingFromSlider = true
			  tf.text = when (p) {
				is CfgIntProp    -> "${floor(value)}"
				is CfgDoubleProp -> "${value.sigFigs(3)}"
				is CfgFloatProp  -> "${value.sigFigs(3)}"
			  }
			  changingFromSlider = false
			}
			changingFromSlider = true
			updateText()
			changingFromSlider = false
			when (responsive) {
			  true  -> {
				valueProperty().onChange {
				  Platform.runLater {
					p.value = if (p is CfgIntProp) floor(it) else it
					updateText()
					onConfigChanged()
				  }
				}
			  }
			  false -> {
				valueChangingProperty().onChange {
				  if (!it) {
					/*I think this runLater is necessary or else the wrong value comes through when snap to ticks is true*/
					Platform.runLater {
					  p.value = if (p is CfgIntProp) floor(value) else it
					  updateText()
					  onConfigChanged()
					}
				  }
				}
			  }
			}
		  }

		  add(tf)


		}


	  }
	}
  }
}