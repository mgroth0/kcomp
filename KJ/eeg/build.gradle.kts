dependencies {
	implementation(projects.kj.gui)
  implementation(libs.brainflow)
}
repositories{
  maven {
	url = uri("https://maven.pkg.github.com/brainflow-dev/brainflow")
	credentials {
	  username = "mgroth0"/*project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")*/
	  password = "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"/*project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")*/
	}
  }
}