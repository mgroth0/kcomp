package matt.sempart.client.sty

import matt.kjs.css.BorderStyle.solid
import matt.kjs.css.BorderWidth.thin
import matt.kjs.css.BoxSizing.borderBox
import matt.kjs.css.MyStyleDsl
import matt.kjs.css.px

fun MyStyleDsl.box() {
  borderStyle = solid
  borderWidth = thin
  //  margin = MED_SPACE
  boxSizing = borderBox
}

val SMALL_SPACE = 5.px
val MED_SPACE = SMALL_SPACE*2

//fun MyStyleDsl.boxButton() {
//  marginBottom = SMALL_SPACE
//  marginTop = SMALL_SPACE
//  marginLeft = SMALL_SPACE
//  display = block
//}