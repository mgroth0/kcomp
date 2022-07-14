implementations {
  remote
  remoteSlurm
  remoteOpenmind
  kjlibJmath
  async
  kjlibGit
  tensorflow()
}
implementations(
  projects.k.nn.keras,

  "org.jetbrains.kotlinx:kotlin-deeplearning-api:0.4.0",
  "org.jetbrains.kotlinx:kotlin-deeplearning-onnx:0.4.0",
  "org.jetbrains.kotlinx:kotlin-deeplearning-visualization:0.4.0"

)
compileOnlys {
  libs.tensorflow /*for navigating sources in IDE*/
}
