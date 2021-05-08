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



tasks {

  val gitUpdateSubmodules by creating(Exec::class) {
	commandLine("git", "submodule", "foreach", "git", "pull")
  }

  val gitCommitSubs = gitSubmodules.map {
	create("gitCommit${it.first.capitalize()}", Exec::class) {
	  mustRunAfter(gitUpdateSubmodules)
	  workingDir(file(it.second))
	  commandLine("git", "commit", "-am", "autocommit")
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
  }

  val gitCommit by creating(Exec::class) {
	mustRunAfter(gitUpdateSubmodules)
	gitCommitSubs.forEach { mustRunAfter(it) }
	commandLine("git", "commit", "-am", "autocommit")
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


  allprojects {
	tasks.withType<Jar> {
	  gitCommit.mustRunAfter(this)
	  gitCommitSubs.forEach { it.mustRunAfter(this) }
	}
  }

  val gitPushSubs = gitSubmodules.map {
	create("gitPush${it.first.capitalize()}", Exec::class) {

	  mustRunAfter("gitCommit${it.first.capitalize()}")

	  workingDir(file(it.second))
	  commandLine("git", "push")
	}
  }

  val gitPush by creating(Exec::class) {
	mustRunAfter(gitCommit)
	gitPushSubs.forEach { mustRunAfter(it) }
	commandLine("git", "push")
  }

  val gitAddCommitPush by creating {
	dependsOn(gitUpdateSubmodules)

	dependsOn(gitCommit)
	dependsOn(gitPush)

	gitCommitSubs.forEach { dependsOn(it) }
	gitPushSubs.forEach { dependsOn(it) }

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




