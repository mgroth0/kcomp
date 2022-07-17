import matt.key.REMOTE_GRADLE_ARG

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
	dependsOn(rootProject.tasks.matching { it.name == "kbuild" })
	args!!.add(REMOTE_GRADLE_ARG)
  }
}

