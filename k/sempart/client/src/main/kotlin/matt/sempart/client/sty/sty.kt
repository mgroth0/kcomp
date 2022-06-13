package matt.sempart.client.sty

import matt.kjs.css.BorderStyle.solid
import matt.kjs.css.BorderWidth.thin
import matt.kjs.css.MyStyleDsl
import matt.kjs.css.px

fun MyStyleDsl.box() {
  borderStyle = solid
  borderWidth = thin
}

fun MyStyleDsl.boxButton() {
  marginBottom = 5.px
  marginTop = 5.px
  marginLeft = 5.px
}