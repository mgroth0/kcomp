@file:Suppress("UnstableApiUsage")


enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


include("KJ")
include("KJ:v1")

val projectFolder = File(System.getProperty("user.dir"))
dependencyResolutionManagement {
  versionCatalogs {
	create("libs") {
	  from(files(projectFolder.resolve("libs.versions.toml")))
	}
  }
}
