package matt.sempart.client.sty

import matt.klib.css.BorderStyle.solid
import matt.klib.css.BorderWidth.thin
import matt.klib.css.BoxSizing.borderBox
import matt.klib.css.CssStyleDSL
import matt.klib.css.px

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