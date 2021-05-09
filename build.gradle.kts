import matt.jbuild.greeting.JGreetingPlugin
import matt.jbuild.jigsaw.JigsawPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val JIGSAW: Boolean by extra(false)
val verbose = false
if (verbose) {
  println("JDK Version: ${JavaVersion.current()}")
}
if (JavaVersion.current() != JavaVersion.VERSION_16) {
  throw GradleException("This build must be run with java 16")
}

plugins {
  id("org.jetbrains.kotlin.multiplatform") apply false
  id("com.github.johnrengelman.shadow") version "6.1.0"
  idea
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

subprojects {
  tasks {
	if (".git" in projectDir.list()) {
	  val gitPullSubmodule by creating(Exec::class) {
		workingDir(projectDir)
		commandLine("git", "pull")
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
		this@subprojects
			.subprojects
			.filter { ".git" in it.projectDir!!.list() }
			.forEach {
			  it.tasks.withType {
				if (it.name == "gitAddCommitSubmodule") {
				  addCommitTask.dependsOn(it)
				}
			  }
			  //			  dependsOn(it.tasks["gitAddCommitSubmodule"])
			}
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
	  val gitPushSub by creating(Exec::class) {
		val pushTask = this
		//		mustRunAfter(gitPullSubmodule)
		//		tasks.withType<Jar> {
		//		  addCommitTask.mustRunAfter(this)
		//		}
		workingDir(projectDir)
		commandLine("git", "push")
	  }
	}
  }
}

tasks {

  //  val gitUpdateSubmodules by creating(Exec::class) {
  //	commandLine("git", "submodule", "foreach", "--recursive", "git", "pull")
  //  }
  //  val gitCommitSubs by creating(Exec::class) {
  //
  //	commandLine("git", "submodule", "foreach", "--recursive", "git add-commit -m autocommit || :")
  //	isIgnoreExitValue = true
  //	this.setStandardOutput(java.io.ByteArrayOutputStream())
  //
  //	doLast {
  //	  val stdout = standardOutput.toString()
  //	  if (execResult!!.exitValue == 0) {
  //		//do nothing
  //	  } else if ("nothing to commit" !in stdout) {
  //		throw RuntimeException(stdout)
  //	  }
  //	}
  //  }

  //  val gitCommitSubs = gitSubmodules.map {
  //	create("gitCommit${it.first.capitalize()}", Exec::class) {
  //	  mustRunAfter(gitUpdateSubmodules)
  //	  workingDir(file(it.second))
  //	  /*https://stackoverflow.com/questions/4298960/git-add-and-commit-in-one-command*/
  //	  commandLine("git", "add-commit", "-m", "autocommit")
  //	  isIgnoreExitValue = true
  //	  this.setStandardOutput(java.io.ByteArrayOutputStream())
  //
  //	  doLast {
  //		val stdout = standardOutput.toString()
  //		if (execResult!!.exitValue == 0) {
  //		  //do nothing
  //		} else if ("nothing to commit" !in stdout) {
  //		  throw RuntimeException(stdout)
  //		}
  //	  }
  //	}
  //  }

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
	commandLine("git", "pull")
	workingDir("buildSrc")
  }
  val gitCommitBuildSrc by creating(Exec::class) {

	mustRunAfter(gitPullBuildSrc)
	//	mustRunAfter(gitCommitSubs)
	//	gitCommitSubs.forEach { mustRunAfter(it) }
	/*https://stackoverflow.com/questions/4298960/git-add-and-commit-in-one-command*/
	commandLine("git", "add-commit", "-m", "autocommit")
	workingDir("buildSrc")
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
  val gitPushBuildSrc by creating(Exec::class) {
	commandLine("git", "push")
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
	commandLine("git", "push")
  }

  val gitAddCommitPush by creating {

	val theTask = this

	subprojects {
	  tasks.withType {
		if (name in listOf("gitPushSub", "gitAddCommitSubmodule", "gitPullSubmodule")) {
		  theTask.dependsOn(this)
		}
	  }
	}

	//	dependsOn(gitUpdateSubmodules)

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
	dependsOn(gitAddCommitPush)
  }
  allprojects {
	tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
	  kbuild.dependsOn(this)
	}
  }
  val mbuild by creating {
	/*project(":kjs").afterEvaluate {
	  dependsOn(":kjs:assemble")
	}*/
	dependsOn(kbuild)
  }


}


val check = tasks.register("validate", MValidations::class)
allprojects {
  tasks.withType<JavaCompile> {
	dependsOn(check)
  }
  tasks.withType<KotlinCompile> {
	dependsOn(check)
	kotlinOptions {
	  mventionKotlinJvmOptions()
	}

	/*https://kotlinlang.org/docs/opt-in-requirements.html#module-wide-opt-in*/
	kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.contracts.ExperimentalContracts"
  }
}

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

  if (JIGSAW) apply<JigsawPlugin>()
  apply<JGreetingPlugin>()
}




