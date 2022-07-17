import matt.auto.copyToFast
import matt.file.jar
import matt.kbuild.gbuild.buildDirM
import matt.kbuild.gbuild.builtShadowJar
import org.panteleyev.jpackage.JPackageTask

apis {

}
implementations {
  gui
  libs.`kotlinx-serialization-cbor`
}

plugins {
  id("org.panteleyev.jpackageplugin") version "1.3.1"
}

tasks.withType(JPackageTask::class) {
  dependsOn(tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>())
  val appInputFolder = buildDirM + "appInput"
  val appOutputFolder = buildDirM + "appOutput"
  doFirst {
	appInputFolder.mkdirs()
	builtShadowJar.copyToFast(appInputFolder + project.name.jar)
  }
  input = appInputFolder.abspath
  mainJar = project.name.jar.name
  destination = appOutputFolder.abspath
  appName = project.name
  /*appVersion = "1.0.0"*/
}