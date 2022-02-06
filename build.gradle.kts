/*this file is hard-linked across 2 projects*/


val verbose = false
if (verbose) {
  println("JDK Version: ${JavaVersion.current()}")
}
//if (JavaVersion.current() != JavaVersion.VERSION_17) {
if (JavaVersion.current() != JavaVersion.VERSION_16) {
  throw GradleException("This build must be run with java 16 (could try 17, but currently trying to make intelliJ resolve core java packages and no idea why its not working)")
}



plugins {
  id("org.jetbrains.kotlin.multiplatform") apply false
  id("com.github.johnrengelman.shadow") version "6.1.0"
  idea /*this was auto inserted by IntelliJ on New Mac but I'll let it be*/
  if ("mac" in System.getProperty("os.name").toLowerCase()) {
	id("com.vanniktech.dependency.graph.generator")
  }
  id("org.barfuin.gradle.taskinfo") version "1.1.1"

}





if (isMac) {
  tasks {

	val openGraph by creating {
	  dependsOn("generateDependencyGraph")
	  doLast {
		println("sending signal to file")
		InterAppInterface["file"].open("/Users/matt/Desktop/registered/todo/flow/build/reports/dependency-graph/dependency-graph.svg")
		println("sent signal")
	  }
	}
  }
}





idea {
  module {
	excludeDirs = setOf(
	  File("data"),
	  File("backups"),
	  File("status"),
	  File("PDF"),
	  File("log"),
	  File("icon"),
	  File("cfg"),
	  File("cache")
	)
  }
}


val check = tasks.register("validate", MValidations::class)
subprojects {
  val thisGitPath = File(this.projectDir.path).resolve(".git")
  tasks {
	//	println("projectDir:${projectDir}")
	if (".git" in projectDir.list()) {
	  val gitCheckoutMasterSub by creating(Exec::class) {
		/*workingDir(projectDir)*/
		/*commandLine("git", "checkout", "master")*/
		commandLine("ls")
		doLast {
		  if ("detached" in shell("git", "--git-dir=${thisGitPath}", "branch")) {
			shell("git", "--git-dir=${thisGitPath}", "add-commit", "-m", "autocommit")
			shell("git", "--git-dir=${thisGitPath}", "branch", "-d", "tmp")
			shell("git", "--git-dir=${thisGitPath}", "branch", "tmp")
			shell("git", "--git-dir=${thisGitPath}", "checkout", "master")
			shell("git", "--git-dir=${thisGitPath}", "merge", "tmp")
		  }
		  shell("git", "--git-dir=${thisGitPath}", "checkout", "master")
		}
	  }
	  val gitPullSubmodule by creating(Exec::class) {
		mustRunAfter(gitCheckoutMasterSub)
		workingDir(projectDir)
		commandLine("git", "pull", "origin", "master")
		if (parent != null && parent != rootProject) {
		  if (".git" in parent!!.projectDir.list()) {
			dependsOn(parent!!.tasks["gitPullSubmodule"])
		  }
		}
	  }
	  val gitAddCommitSubmodule by creating(Exec::class) {
		val addCommitTask = this
		mustRunAfter(gitPullSubmodule)
		this@subprojects.tasks.withType<Jar> {
		  addCommitTask.mustRunAfter(this)
		}
		workingDir(projectDir)
		/*https://stackoverflow.com/questions/4298960/git-add-and-commit-in-one-command*/
		/*https://stackoverflow.com/questions/19728933/continue-looping-over-submodules-with-the-git-submodule-foreach-command-after*/
		commandLine("git", "add-commit", "-m", "autocommit")
		dependsOn(check)
		this@subprojects
		  .subprojects
		  .filter {
			".git" in it.projectDir!!.list()
		  }
		  .forEach {
			it.tasks.withType {
			  if (name == "gitAddCommitSubmodule") {
				addCommitTask.dependsOn(this)
			  }
			}
		  }
		isIgnoreExitValue = true
		this.standardOutput = java.io.ByteArrayOutputStream()

		doLast {
		  val stdout = standardOutput.toString()
		  if (execResult!!.exitValue == 0) {
			//do nothing
		  } else if ("nothing to commit" !in stdout) {
			throw RuntimeException(stdout)
		  }
		}
	  }
	  val gitPushSub by creating(Exec::class) {
		val pushTask = this
		//		mustRunAfter(gitPullSubmodule)
		//		tasks.withType<Jar> {
		//		  addCommitTask.mustRunAfter(this)
		//		}
		workingDir(projectDir)
		commandLine("git", "push", "origin", "master")
		mustRunAfter(gitAddCommitSubmodule)
	  }
	}
	Unit
  }
}

val ROOT_FILES_FOLDER = File("RootFiles")

tasks {

  val syncRootFiles by creating(Task::class) {
	doLast {

	  ROOT_FILES_FOLDER.listFiles()!!.filter {
		".DS" !in it.name
			&& !it.isDirectory
	  }.forEach { rootFileInFolder ->
		var needRestart = false
		val rootFileInRoot = File(rootFileInFolder.name)
		if (rootFileInFolder.readText() != rootFileInRoot.readText()) {
		  if (rootFileInFolder.lastModified() > rootFileInRoot.lastModified()) {
			rootFileInRoot.writeText(rootFileInFolder.readText())
			needRestart = true
		  } else if (rootFileInRoot.lastModified() > rootFileInFolder.lastModified()) {
			rootFileInFolder.writeText(rootFileInRoot.readText())
		  } else err("wow mtimes are same but text not equal")
		}
		if (needRestart) {
		  throw GradleException("need to restart because a root file was updated")
		}

		val rootFilesPath = ROOT_FILES_FOLDER.absolutePath

		if ("detached" in shell("git", "--git-dir=${rootFilesPath}", "branch")) {
		  shell("git", "--git-dir=${rootFilesPath}", "add-commit", "-m", "autocommit")
		  shell("git", "--git-dir=${rootFilesPath}", "branch", "-d", "tmp")
		  shell("git", "--git-dir=${rootFilesPath}", "branch", "tmp")
		  shell("git", "--git-dir=${rootFilesPath}", "checkout", "master")
		  shell("git", "--git-dir=${rootFilesPath}", "merge", "tmp")
		}
		shell("git", "--git-dir=${rootFilesPath}", "checkout", "master")


		shell("git", "--git-dir=${rootFilesPath}", "add-commit", "-m", "autocommit")
		shell("git", "--git-dir=${rootFilesPath}", "pull", "origin", "master")
		shell("git", "--git-dir=${rootFilesPath}", "push", "origin", "master")


		needRestart = false
		if (rootFileInFolder.readText() != rootFileInRoot.readText()) {
		  if (rootFileInFolder.lastModified() > rootFileInRoot.lastModified()) {
			rootFileInRoot.writeText(rootFileInFolder.readText())
			needRestart = true
		  } else if (rootFileInRoot.lastModified() > rootFileInFolder.lastModified()) {
			rootFileInFolder.writeText(rootFileInRoot.readText())
		  } else err("wow mtimes are same but text not equal")
		}
		if (needRestart) {
		  throw GradleException("need to restart because a root file was updated")
		}


	  }


	}
  }


  val gitPull by creating(Exec::class) {
	commandLine("git", "pull", "origin", "master")
  }
  val gitCommit by creating(Exec::class) {
	val gcTask = this
	subprojects {
	  tasks.withType {
		if (name in listOf("gitAddCommitSubmodule", "gitPullSubmodule")) {
		  gcTask.mustRunAfter(this)
		}
	  }
	}
	//	mustRunAfter(gitUpdateSubmodules)
	//	mustRunAfter(gitCommitSubs)
	//	gitCommitSubs.forEach { mustRunAfter(it) }
	/*https://stackoverflow.com/questions/4298960/git-add-and-commit-in-one-command*/
	commandLine("git", "add-commit", "-m", "autocommit")
	dependsOn(check)
	isIgnoreExitValue = true
	this.setStandardOutput(java.io.ByteArrayOutputStream())

	doLast {
	  val stdout = standardOutput.toString()
	  if (execResult!!.exitValue == 0) {
		//do nothing
	  } else if ("nothing to commit" !in stdout) {
		throw RuntimeException(stdout)
	  }
	}
  }
  val gitPullBuildSrc by creating(Exec::class) {
	commandLine("git", "pull", "origin", "master")
	workingDir("buildSrc")
  }
  val gitCommitBuildSrc by creating(Exec::class) {

	mustRunAfter(gitPullBuildSrc)
	//	mustRunAfter(gitCommitSubs)
	//	gitCommitSubs.forEach { mustRunAfter(it) }
	/*https://stackoverflow.com/questions/4298960/git-add-and-commit-in-one-command*/
	commandLine("git", "add-commit", "-m", "autocommit")
	dependsOn(check)
	workingDir("buildSrc")
	isIgnoreExitValue = true
	this.standardOutput = java.io.ByteArrayOutputStream()

	doLast {
	  val stdout = standardOutput.toString()
	  if (execResult!!.exitValue == 0) {
		//do nothing
	  } else if ("nothing to commit" !in stdout) {
		throw RuntimeException(stdout)
	  }
	}
  }
  val gitPushBuildSrc by creating(Exec::class) {
	commandLine("git", "push", "origin", "master")
	workingDir("buildSrc")
	mustRunAfter(gitCommitBuildSrc)
  }



  allprojects {
	tasks.withType<Jar> {
	  gitCommit.mustRunAfter(this)
	  //	  gitCommitSubs.mustRunAfter(this)
	  //	  gitCommitSubs.forEach { it.mustRunAfter(this) }
	}
  }
  //  val gitPushSubs by creating(Exec::class) {
  //	commandLine("git", "submodule", "foreach", "--recursive", "git", "push")
  //  }

  //  val gitPushSubs = gitSubmodules.map {
  //	create("gitPush${it.first.capitalize()}", Exec::class) {
  //
  //	  mustRunAfter("gitCommit${it.first.capitalize()}")
  //
  //	  workingDir(file(it.second))
  //	  commandLine("git", "push")
  //	}
  //  }

  val gitPush by creating(Exec::class) {
	mustRunAfter(gitCommit)
	val pushTask = this
	subprojects {
	  tasks.withType {
		if (name in listOf("gitPushSub")) {
		  pushTask.mustRunAfter(this)
		}
	  }
	}
	//	mustRunAfter(gitPushSubs)
	//	gitPushSubs.forEach { mustRunAfter(it) }
	commandLine("git", "push", "origin", "master")
  }

  val gitAddCommitPush by creating {
	mustRunAfter(syncRootFiles)
	val theTask = this

	subprojects {
	  tasks.withType {
		if (name in listOf("gitPushSub", "gitAddCommitSubmodule", "gitPullSubmodule", "gitCheckoutMasterSub")) {
		  theTask.dependsOn(this)
		}
	  }
	}

	//	dependsOn(gitUpdateSubmodules)

	dependsOn(gitPull)
	dependsOn(gitCommit)
	dependsOn(gitPush)

	dependsOn(gitPullBuildSrc)
	dependsOn(gitCommitBuildSrc)
	dependsOn(gitPushBuildSrc)

	//	dependsOn(gitCommitSubs)
	//	dependsOn(gitPushSubs)
	//	gitCommitSubs.forEach { dependsOn(it) }
	//	gitPushSubs.forEach { dependsOn(it) }

  }

  val kbuild by creating {
	dependsOn(syncRootFiles)
	dependsOn(gitAddCommitPush)
  }
  allprojects {
	tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
	  mustRunAfter(syncRootFiles)
	  kbuild.dependsOn(this)
	}
	tasks.withType<PublishToMavenRepository> {
	  mustRunAfter(syncRootFiles)
	  kbuild.dependsOn(this)
	}
  }
  val mbuild by creating {
	allprojects.firstOrNull { it.path == ":kjs" }?.also {
	  it.afterEvaluate {
		dependsOn(":kjs:assemble")
	  }
	}
	dependsOn(kbuild)
  }


}
allprojects {
  tasks.withType<JavaCompile> {
	dependsOn(check)
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	dependsOn(check)
	kotlinOptions {
	  mventionKotlinJvmOptions()
	}

	/*https://kotlinlang.org/docs/opt-in-requirements.html#module-wide-opt-in*/
	kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.contracts.ExperimentalContracts"
  }
}


val JIGSAW: Boolean by extra(false)

subprojects {

  val thisProj = this

  tasks {
	@Suppress("unused")
	val fixJigsaw by creating {
	  doLast {
		val moved = thisProj.projectDir.resolve("module-info.java")
		val movedNew = when (thisProj.name) {
		  "klib" -> thisProj.projectDir.resolve("src/jvmMain/module-info.java")
		  else   -> thisProj.projectDir.resolve("src/main/module-info.java")
		}
		val inplace = when (thisProj.name) {
		  "klib" -> thisProj.projectDir.resolve("src/jvmMain/java/module-info.java")
		  else   -> thisProj.projectDir.resolve("src/main/java/module-info.java")
		}
		if (moved.exists()) {
		  movedNew.parentFile.mkdirs()
		  moved.renameTo(movedNew)
		}
		if (JIGSAW && moved.exists()) {
		  inplace.parentFile.mkdirs()
		  moved.renameTo(inplace)
		} else if (!JIGSAW && inplace.exists()) {
		  inplace.parentFile.mkdirs()
		  inplace.renameTo(moved)
		}
	  }
	}
  }


  if (JIGSAW) apply<matt.jbuild.jigsaw.JigsawPlugin>()
  apply<matt.jbuild.greeting.JGreetingPlugin>()
}


//buildscript {
//
//  repositories {
//	//	google() /*android*/
//	//	mavenCentral() /*android*/
//  }
//
//  dependencies {
//	classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
//	//	classpath("com.android.tools.build:gradle:7.0.3") /*android*/
//  }
//
//
//}


