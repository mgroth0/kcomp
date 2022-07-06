package matt.sempart.client.sty

import matt.css.BorderStyle.solid
import matt.css.BorderWidth.thin
import matt.css.BoxSizing.borderBox
import matt.css.CssStyleDSL
import matt.css.px

fun CssStyleDSL.box() {
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