@file:Suppress("UnstableApiUsage")


enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val projectFolder = File(System.getProperty("user.dir"))

projectFolder
	.listFiles()!!
	.filter { "build.gradle.kts" in it.listFiles()?.map { it.name } ?: listOf() }
	.filter { it.name != "buildSrc" }
	.forEach {
	  include(it.name)
	}

projectFolder.resolve("KJ")
	.listFiles()!!
	.filter { "build.gradle.kts" in it.listFiles()?.map { it.name } ?: listOf() }
	.filter { it.name != "buildSrc" }
	.forEach {
	  include("KJ:" + it.name)

	  it.listFiles()!!
		  .filter {
			"build.gradle.kts" in it.listFiles()?.map { it.name } ?: listOf()
		  }
		  .filter { it.name != "buildSrc" }
		  .forEach { f ->
			include("KJ:${f.parentFile.name}:" + f.name)
		  }
	}


dependencyResolutionManagement {
  versionCatalogs {
	create("libs") {
	  from(files(projectFolder.resolve("libs.versions.toml")))
	}
  }
}
