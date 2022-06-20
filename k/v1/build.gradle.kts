modtype = APP

dependencies {
  implementation(projects.k.gui)
//  implementation(libs.bundles.multik.full)
  implementation(libs.kotlinx.multik.api)
  implementation(libs.mat)
  implementation(projects.k.fx.fxGraphics)
  api(projects.k.caching)
  api(projects.k.async)
  implementation(libs.commons.math)
  implementation(libs.chartFX)
  implementation(libs.apfloat)
  implementation(libs.fx.swing)
  implementation(projects.k.remote)
  implementation(projects.k.fxlib.fxlibConsole)
  implementation(projects.k.dataman)
  implementation(projects.k.kjlib.kjlibJmath)
  implementation(projects.k.reflect)
  implementation(projects.k.fx.fxGraphics)
}

plugins {
  kotlin("plugin.serialization")
}