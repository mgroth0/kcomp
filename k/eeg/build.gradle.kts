dependencies {
  implementation(projects.k.gui)
  implementation(projects.k.kjlib.shell)
  compileOnly(libs.brainflow) /*for source navigation?*/
  implementation(files(matt.file.commons.RootProjects.kcomp.folder + "k" + "jar" + "brainflow-jar-with-dependencies.jar"))
}
repositories {
  maven {
	url = uri("https://maven.pkg.github.com/brainflow-dev/brainflow")
	credentials {
	  /*https://github.com/settings/tokens*/
	  username = "mgroth0"/*project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")*/
	  password = "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" /*"Read Packages Forever"*/ /*project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")*/
	}
  }
}