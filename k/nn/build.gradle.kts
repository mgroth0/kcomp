import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import matt.remote.runOnOM

implementations {
  kjlibJmath
  async
}
implementations(
  libs.kotlindl.api,
  libs.bundles.kotlindl.gpu
)


tasks {
  //  val remoteGradleRun by registering(JavaExec::class) {
  //	classpath = sourceSets.main.get().runtimeClasspath
  //	mainClass.set(JavaExecutable(project).mainClass)
  //	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
  //	args = listOf(REMOTE_GRADLE_ARG)
  //  }
  val remoteGradleRun by registering {
	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
	doLast {
	  val OMMachine = matt.remote.openmind.Polestar
	  val srun = if (OMMachine != matt.remote.openmind.Polestar) matt.remote.slurm.SRun(timeMin = 15) else null
	  OMMachine.ssh {
		runOnOM(matt.remote.GradleTaskExec("k:nn:run"), srun = srun)
	  }
	}
  }
  val remoteShadowRun by registering {
	dependsOn(withType<ShadowJar>())
	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
	doLast {
	  val OMMachine = matt.remote.openmind.Polestar
	  val srun = if (OMMachine != matt.remote.openmind.Polestar) matt.remote.slurm.SRun(timeMin = 15) else null
	  OMMachine.ssh {
		runOnOM(matt.remote.ShadowJarExec("nn"), srun = srun)
	  }
	}
  }
}

