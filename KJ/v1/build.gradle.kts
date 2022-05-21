dependencies {
  implementation(projects.kj.gui)
  implementation(libs.bundles.multik.full)
  /*implementation(libs.aparapi)*/
  implementation(libs.mat)
  /*println("testing if newMac")*/
  /*    if (isNewMac) {
		  println("ok this is working...")
		  implementation(
				  files(
						  "/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi-natives.jar",
						  "/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi.jar",
						  "/Users/matthewgroth/registered/kcomp/KJ/jar/bcel-6.0.jar"
				  )
		  )
	  } else if (isMac) {
		  implementation(
				  files(
						  "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi-natives.jar",
						  "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi.jar",
						  "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/bcel-6.0.jar"
				  )
		  )
	  } else {*/
  implementation(libs.aparapi)
  /*}*/
  implementation(libs.commons.math)
  implementation(libs.chartFX)
  implementation(libs.apfloat)
  implementation(libs.fx.swing)
  implementation(projects.kj.remote)
  implementation(projects.kj.fxlib.console)
  implementation(projects.kj.dataman)

  /*implementation("org.tensorflow:tensorflow-core-api:0.4.0")*/
  /*implementation("org.tensorflow:tensorflow-core-platform:0.4.0")*/
  /*implementation(
	files(
	  "/Users/matthewgroth/registered/kcomp/KJ/jar/tensorflow-core-api-0.4.0-SNAPSHOT.jar",
	  "/Users/matthewgroth/registered/kcomp/KJ/jar/tensorflow-core-api-0.4.0-SNAPSHOT-macosx-arm64.jar",
	  "/Users/matthewgroth/registered/kcomp/KJ/jar/tensorflow-core-generator-0.4.0-SNAPSHOT.jar",
	  "/Users/matthewgroth/registered/kcomp/KJ/jar/tensorflow-core-platform-0.4.0-SNAPSHOT.jar",
	  "/Users/matthewgroth/registered/kcomp/KJ/jar/tensorflow-framework-0.4.0-SNAPSHOT.jar",
	)
  )*/



}

/*
configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("org.tensorflow:tensorflow-core-api"))
      .using(module("org.tensorflow:tensorflow-core-api:0.4.0"))
      .withClassifier("macosx-x86_64")
  }
}*/


plugins {
  kotlin("plugin.serialization") version tomlVersion("kotlin")
}