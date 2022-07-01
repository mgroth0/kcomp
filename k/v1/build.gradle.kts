modtype = APP

plugins {
  kotlin("plugin.serialization")
}
implementations(
  projects.k.gui,
  libs.kotlinx.multik.api,
  libs.mat,
  projects.k.fx.fxGraphics,
  projects.k.caching,
  projects.k.async,
  libs.commons.math,
  libs.chartFX,
  libs.apfloat,
  libs.fx.swing,
  projects.k.remote,
  projects.k.fxlib.fxlibConsole,
  projects.k.dataman,
  projects.k.kjlib.kjlibJmath,
  projects.k.reflect,
  projects.k.fx.fxGraphics,
  libs.slf4j.nop
)