import matt.klib.commons.RootProjects.kcomp
import matt.klib.commons.plus

modtype = APP
dependencies {
  implementation(projects.k.gui)
  implementation(projects.k.kjlib.kjlibShell)
  //  implementation(libs.brainflow
  //  )
  implementation(files(kcomp.folder + "k/jar/brainflow-jar-with-dependencies.jar"))
}
repositories {
  maven {
	url = uri("https://maven.pkg.github.com/brainflow-dev/brainflow")
	credentials {
	  username = "mgroth0"/*project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")*/
	  password =
		"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"/*project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")*/
	}
  }
}