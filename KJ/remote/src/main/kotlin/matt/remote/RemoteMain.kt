package matt.remote

import matt.exec.cmd.CommandLineApp
import matt.remote.expect.cd
import matt.remote.expect.exit
import matt.remote.expect.hostname
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

const val REBUILD_SINGULARITY = false

fun main() = CommandLineApp("Hello remote") {

  Hosts.POLESTAR.ssh {

	mkdir(OM_KCOMP)
	cd(OM_KCOMP)
	pwd()
	ls()

	sendLineAndWait("git pull")
	sendLineAndWait("git submodule update")

	writeFile(VAGRANTFILE_NAME, VagrantfileForSingularityBuild(OM_KCOMP))
	sendLineAndWait("module load openmind/singularity")
	hostname()
	sendLine(SRun(timeMin = 15).command)
	/*srun -v -c 1 --mem=4G  -t 15 --pty bash*/
	/*srun -v -c 1 --mem=4G --gres=gpu:0 -t 15 --pty bash*/
	/*srun -v -c 1 --mem=4G --gres=gpu:0 --constraint=any-gpu -t 15 --pty bash*/
	setPrompt()
	hostname()

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
	/*singularity exec -B /om2/user/mjgroth/kcomp:/om2/user/mjgroth/kcomp kcomp.simg /bin/bash*/
	setPrompt(numExpectPrompts = 2)
	hostname()
	cd(OM_KCOMP)
	sendLineAndWait("Xvfb :0 -screen 0 1600x1200x16 &")
	sendLineAndWait("export DISPLAY=:0")
	sendLineAndWait("/opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle KJ:v1:run")
	/*/opt/gradle/gradle-7.0.2/bin/gradle KJ:v1:run*/
	/*javac java.java*/
	/*java HelloWorld*/
	interact()
	pwd()
	ls()
	rm(SINGULARITY_FILE_NAME)
	rm(VAGRANTFILE_NAME)

  }

  acceptAny { println("command=$it") }
}.start()

