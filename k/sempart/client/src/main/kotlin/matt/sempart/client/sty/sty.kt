package matt.sempart.client.sty

import matt.kjs.css.BorderStyle.solid
import matt.kjs.css.BorderWidth.thin
import matt.kjs.css.Display.block
import matt.kjs.css.MyStyleDsl
import matt.kjs.css.Position.absolute
import matt.kjs.css.Transform
import matt.kjs.css.percent
import matt.kjs.css.px

fun MyStyleDsl.box() {
  borderStyle = solid
  borderWidth = thin
  margin = MED_SPACE
}

val SMALL_SPACE = 5.px
val MED_SPACE = SMALL_SPACE*2

fun MyStyleDsl.boxButton() {
  marginBottom = SMALL_SPACE
  marginTop = SMALL_SPACE
  marginLeft = SMALL_SPACE
  display = block
}

/*https://stackoverflow.com/a/31029494/6596010*/
fun MyStyleDsl.centerOnWindow() {
  position = absolute
  top = 50.percent
  left = 50.percent
  transform = Transform().apply {
	map["translate"] = listOf("-50%", "-50%")
  }
}