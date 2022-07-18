implementations {
//  remote
//  remoteSlurm
  remoteOpenmind
  kjlibJmath
  async
  kjlibGit
  tensorflow()
}
implementations(
  projects.k.nn.keras,
)
compileOnlys {
  libs.tensorflow /*for navigating sources in IDE*/
}
