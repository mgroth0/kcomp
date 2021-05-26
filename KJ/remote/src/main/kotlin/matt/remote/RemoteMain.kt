package matt.remote

import matt.exec.cmd.CommandLineApp
import matt.remote.expect.cd
import matt.remote.expect.exit
import matt.remote.expect.ls
import matt.remote.expect.mkdir
import matt.remote.expect.pwd
import matt.remote.expect.rm
import matt.remote.expect.sendLineAndWait
import matt.remote.expect.writeFile
import matt.remote.host.Hosts
import matt.remote.host.setPrompt
import matt.remote.om.OM.OM_DATA_FOLD
import matt.remote.om.OM.OM_KCOMP
import matt.remote.singularity.SINGULARITY_FILE_NAME
import matt.remote.singularity.SINGULARITY_SIMG_NAME
import matt.remote.singularity.SingularityRecipe
import matt.remote.singularity.SingularityRecipe.Companion.GRADLE_VERSION
import matt.remote.slurm.SRun
import matt.remote.vagrant.VAGRANTFILE_NAME
import matt.remote.vagrant.VagrantfileForSingularityBuild

const val REBUILD_SINGULARITY = true

fun main() = CommandLineApp("Hello remote") {

  Hosts.POLESTAR.ssh {

	mkdir(OM_KCOMP)
	cd(OM_KCOMP)
	pwd()
	ls()

	writeFile(VAGRANTFILE_NAME, VagrantfileForSingularityBuild(OM_KCOMP))
	sendLineAndWait("module load openmind/singularity")
	sendLine(SRun(timeMin = 15).command)
	setPrompt()

	if (REBUILD_SINGULARITY) {
	  sendLineAndWait("vagrant up")
	  sendLine("vagrant ssh")
	  setPrompt()
	  cd(OM_KCOMP)
	  writeFile(SINGULARITY_FILE_NAME, SingularityRecipe().text)
	  rm(SINGULARITY_SIMG_NAME)
	  sendLineAndWait("sudo singularity -v build --writable $SINGULARITY_SIMG_NAME $SINGULARITY_FILE_NAME")
	  exit()
	}

	sendLine("singularity exec -B $OM_KCOMP:$OM_KCOMP -B $OM_DATA_FOLD:$OM_DATA_FOLD --nv $SINGULARITY_SIMG_NAME /bin/bash")
	setPrompt(numExpectPrompts = 2)
	cd(OM_KCOMP)
	sendLineAndWait("/opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle KJ:v1:run")
	pwd()
	ls()
	rm(SINGULARITY_FILE_NAME)
	rm(VAGRANTFILE_NAME)

  }

  acceptAny { println("command=$it") }
}.start()

