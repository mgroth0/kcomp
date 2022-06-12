modtype = APP

dependencies {
  implementation(projects.kj.gui)
//  implementation(libs.bundles.multik.full)
  implementation(libs.kotlinx.multik.api)
  implementation(libs.mat)
  implementation(projects.kj.fx.fxGraphics)
  api(projects.kj.caching)
  api(projects.kj.async)
  implementation(libs.commons.math)
  implementation(libs.chartFX)
  implementation(libs.apfloat)
  implementation(libs.fx.swing)
  implementation(projects.kj.remote)
  implementation(projects.kj.fxlib.fxlibConsole)
  implementation(projects.kj.dataman)
  implementation(projects.kj.kjlib.kjlibJmath)
  implementation(projects.kj.reflect)
  implementation(projects.kj.fx.fxGraphics)
}

plugins {
  kotlin("plugin.serialization")
}