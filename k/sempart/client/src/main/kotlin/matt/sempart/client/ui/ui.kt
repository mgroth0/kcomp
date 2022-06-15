package matt.sempart.client.ui

import kotlinx.html.ButtonType
import matt.kjs.css.sty
import matt.kjs.elements.button
import matt.sempart.client.sty.boxButton
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

fun HTMLDivElement.boxButton(op: HTMLButtonElement.()->Unit = {}) = button {
  type = ButtonType.button.realValue
  sty.boxButton()
}