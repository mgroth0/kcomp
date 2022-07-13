import matt.file.DSStoreFile
import matt.klib.sys.NEW_MAC

implementations {
  remote
  remoteSlurm
  remoteOpenmind
  kjlibJmath
}
compileOnlys {
  libs.tensorflow /*for navigating sources in IDE*/
}
implementations(
  when (matt.klib.commons.thisMachine) {
	is NEW_MAC -> fileTree((matt.file.commons.JAR_FOLDER + "tf").apply {
	  val needIn = "-${libs.tensorflow.get().versionConstraint}-SNAPSHOT"
	  require(listFiles()!!
		.filter { it !is DSStoreFile }
		.all { needIn in it.name }) {
		"this check is to ensure that I'm using the same tf version on all machines"
	  }
	})

	else       -> libs.tensorflow
  },
)
