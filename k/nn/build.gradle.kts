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

  val OMMachine = matt.remote.openmind.Polestar

  val remoteGradleRun by registering {
	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
	doLast {
	  OMMachine.ssh {
		runOnOM(matt.remote.GradleTaskExec("k:nn:run"), srun = OMMachine.standardSRun, rebuildSingularity = true) /*temp rebuild sing for bug*/
	  }
	}
  }
  val remoteShadowJar by registering {
	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
	doLast {
	  OMMachine.ssh {
		runOnOM(matt.remote.GradleTaskExec("k:nn:shadowJar"), srun = OMMachine.standardSRun)
	  }
	}
  }
  val remoteShadowRun by registering {
	dependsOn(withType<ShadowJar>())
	doLast {
	  OMMachine.ssh {
		runOnOM(matt.remote.ShadowJarExec("nn", args = listOf("4")), srun = OMMachine.standardSRun)
	  }
	}
  }

  /*https://github.com/Kotlin/kotlindl/blob/8a971163c045c290780ac1ef17c97851c02a4ebb/README.md#fat-jar-issue*/
  withType<ShadowJar> {
	manifest {
	  attributes(Pair("Implementation-Version", "1.15"))
	}
  }
}

