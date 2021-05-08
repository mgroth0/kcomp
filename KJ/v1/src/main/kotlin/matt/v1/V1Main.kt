package matt.v1

import matt.klib.dmap.withStoringDefault

fun main() {
  println("hello v1!")
  mutableMapOf<String, Int>().withStoringDefault { 1 } /*yay*/
}