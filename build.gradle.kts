import matt.file.toMFile
import matt.kbuild.gbuild.depinfo.setupDepInfoTask
import matt.kbuild.gbuild.rootDirM
import matt.kbuild.root.addAtypicalTasksToAllProjects
import matt.kbuild.root.checkVersionsAndProperties
import matt.kbuild.root.configureIdeaExcludes
import matt.kbuild.root.mustRunAfterAllCleans
import matt.kbuild.root.setAllProjectsVersionsToGroupAndSysTime
import matt.kbuild.root.standardizeSubprojectGroupNamesAndNames
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val thisFile = buildscript.sourceFile!!.toMFile()
require(thisFile.hardLinkCount == 2)
plugins {
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("com.dorongold.task-tree") version "2.1.0"
  /*need to reference absolute path here because gradle API in KJG doesn't switch to the right working dir*/
  val stupidKtVersion = File("/Users/matthewgroth/registered/ide/flow/RootFiles/libs.versions.toml").readText()
	.substringAfter("kotlin").substringAfter('"').substringBefore('"')
  kotlin("plugin.serialization") version stupidKtVersion apply false
  idea
}

val ktversion = tomlVersion("kotlin")
logger.info("using gradle version ${gradle.gradleVersion}")
val tomlGradleVersion = tomlVersion("gradle")
tasks.wrapper {/*Use Gradle from: "gradle-wrapper.properties' file*/
  doFirst { (rootDirM + "gradle" + "wrapper").listFiles()!!.forEach { it.deleteRecursively() } /*yup, necessary*/ }
  distributionType = Wrapper.DistributionType.ALL
  logger.info("root wrapper gradle version set to $tomlGradleVersion")
  gradleVersion = tomlGradleVersion
}
checkVersionsAndProperties()
standardizeSubprojectGroupNamesAndNames()
setAllProjectsVersionsToGroupAndSysTime(except = "idea")
addAtypicalTasksToAllProjects()
allprojects { configureIdeaExcludes() }
val root = matt.kbuild.root.RootProject(rootProject)
root.addTypicalGitTasksToAppropriateProjects()
root.addRootFilesSyncAndCompleteGitTasks()
val allChecks = root.addCheckTasksToAllProjects()
root.configureJavaAndKotlinCompileInAllProjects()
root.tryToSilenceNPM()
setupDepInfoTask()
root.setupKBuildTask(allChecks)
root.tryToFixCleanBug(allChecks)

/*https://youtrack.jetbrains.com/issue/KT-50848/KotlinJS-inner-build-routines-are-using-vulnerable-NPM-dependencies-and-now-that-we-have-kotlin-js-store-github-audit-this*/
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
  rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().lockFileDirectory =
	rootProject.projectDir
}



allprojects {
  tasks.withType<KotlinCompile>().all {
	mustRunAfterAllCleans()
  }
}