modtype = APP

dependencies {
  implementation(projects.kj.gui)
  implementation(libs.bundles.multik.full)
  implementation(libs.mat)
  implementation(projects.kj.fx.graphics)
  api(projects.kj.caching)
  api(projects.kj.async)
  implementation(libs.commons.math)
  implementation(libs.chartFX)
  implementation(libs.apfloat)
  implementation(libs.fx.swing)
  implementation(projects.kj.remote)
  implementation(projects.kj.fxlib.console)
  implementation(projects.kj.dataman)
  implementation(projects.kj.kjlib.jmath)
}

plugins {
  kotlin("plugin.serialization")
}