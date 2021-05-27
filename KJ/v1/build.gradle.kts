dependencies {
  implementation(projects.kj.gui)
  implementation(libs.bundles.multik.full)
  /*implementation(libs.aparapi)*/
  if (isMac) {
	implementation(
	  files(
		"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi-natives.jar",
		"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi.jar",
		"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/bcel-6.0.jar"
	  )
	)
  } else {
	implementation(libs.aparapi)
  }
  implementation(libs.commons.math)
  implementation(libs.chartFX)
  implementation(libs.apfloat)
}