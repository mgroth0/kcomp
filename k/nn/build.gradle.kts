implementations {
  remote
  remoteSlurm
  remoteOpenmind
  kjlibJmath
  async
  kjlibGit
}
implementations(
  libs.kotlindl.api,
  libs.bundles.kotlindl.gpu
)

tasks {
  val remoteGradleRun by registering(JavaExec::class) {
	dependsOn(rootProject.tasks.matching { it.name == "kbuild" })
	args!!.add("REMOTE_GRADLE")
  }
}

