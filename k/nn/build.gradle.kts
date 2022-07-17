import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import matt.kbuild.root.mod.JavaExecutable
import matt.key.REMOTE_GRADLE_ARG
import matt.key.REMOTE_SHADOW_ARG

implementations {
  remote
  remoteSlurm
  remoteOpenmind
  kjlibJmath
  async
  kjlibGit
  key
}
implementations(
  libs.kotlindl.api,
  libs.bundles.kotlindl.gpu
)

tasks {
  val remoteGradleRun by registering(JavaExec::class) {
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set(JavaExecutable(project).mainClass)
	dependsOn(rootProject.tasks.matching { it.name == matt.kbuild.root.KBUILD_TASK_NAME })
	args!!.add(REMOTE_GRADLE_ARG)
  }
  val remoteShadowRun by registering(JavaExec::class) {
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set(JavaExecutable(project).mainClass)
	dependsOn(withType<ShadowJar>())
	args!!.add(REMOTE_SHADOW_ARG)
  }
}

