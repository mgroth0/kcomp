/*this file is hard-linked across 2 projects*/

@file:Suppress("UnstableApiUsage")


enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

if ("mac" in System.getProperty("os.name").toLowerCase()) {
  pluginManagement {
	includeBuild("../gradle-dependency-graph-generator-plugin")
  }
}

val projectFolder = File(System.getProperty("user.dir"))

val mightHaveDirectSubprojects = mutableListOf(projectFolder)
while (mightHaveDirectSubprojects.isNotEmpty()) {
  mightHaveDirectSubprojects.toList().apply {
	mightHaveDirectSubprojects.clear()
  }.forEach {
	it
		.listFiles()!!
		.filter { "build.gradle.kts" in (it.list() ?: arrayOf()) }
		.filter { it.name != "buildSrc" }
		.forEach {
		  include(it.relativeTo(projectFolder).path.replace(File.separator, ":"))
		  mightHaveDirectSubprojects.add(it)
		}
  }
}

dependencyResolutionManagement {
  versionCatalogs {
	create("libs") {
	  from(files(projectFolder.resolve("libs.versions.toml")))
	}
  }
}

