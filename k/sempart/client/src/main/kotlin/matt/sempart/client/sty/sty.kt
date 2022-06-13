package matt.sempart.client.sty

import matt.kjs.css.BorderStyle.solid
import matt.kjs.css.BorderWidth.thin
import matt.kjs.css.MyStyleDsl

fun MyStyleDsl.box() {
  borderStyle = solid
  borderWidth = thin
}