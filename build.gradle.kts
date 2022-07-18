import matt.file.toMFile
import matt.kbuild.gbuild.depinfo.setupDepInfoTask
import matt.kbuild.root.addAtypicalTasksToAllProjects
import matt.kbuild.root.checkVersionsAndProperties
import matt.kbuild.root.configureIdeaExcludes
import matt.kbuild.root.setAllProjectsVersionsToGroupAndSysTime
import matt.kbuild.root.standardizeSubprojectGroupNamesAndNames
import matt.klib.sys.Mac
import matt.klib.sys.Linux
import matt.klib.sys.Windows

if (thisMachine is Linux || thisMachine is Windows) GIT = false

val thisFile = buildscript.sourceFile!!.toMFile()
if (thisMachine == NEW_MAC) require(thisFile.hardLinkCount == 2)
plugins {
  //  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("com.dorongold.task-tree") version "2.1.0"
  id("com.github.johnrengelman.shadow")

  //  val stupidKtVersion = File("/Users/matthewgroth/registered/ide/flow/RootFiles/libs.versions.toml").readText()
  //	.substringAfter("kotlin").substringAfter('"').substringBefore('"')
  kotlin("plugin.serialization") version libs.versions.kotlin apply false
}
pluginManager.apply {
  //  apply("com.github.johnrengelman.shadow") // version "7.1.2"
  //  apply("com.dorongold.task-tree") /*version "2.1.0"*/
  /*need to reference absolute path here because gradle API in KJG doesn't switch to the right working dir*/


  apply("idea")
}
//plugins {
//  idea
//}
val ktversion = tomlVersion("kotlin")
logger.info("using gradle version ${gradle.gradleVersion}")
val root = matt.kbuild.root.RootProject(rootProject)
root.configureWrapper()
checkVersionsAndProperties()
standardizeSubprojectGroupNamesAndNames()
setAllProjectsVersionsToGroupAndSysTime(except = "idea")
addAtypicalTasksToAllProjects()
if (thisMachine is Mac) {
  allprojects { configureIdeaExcludes() }
  root.addTypicalGitTasksToAppropriateProjects()
  root.addRootFilesSyncAndCompleteGitTasks()
}
val allChecks = root.addCheckTasksToAllProjects()
root.configureJavaAndKotlinCompileInAllProjects()
root.tryToSilenceNPM()
setupDepInfoTask()
root.setupKBuildTask(allChecks)
root.tryToFixCleanBug(allChecks)
root.moveYarnLock()