package matt.remote.vagrant

import java.io.File


fun VagrantfileForSingularityBuild(buildFolder: File) = """
  # -*- mode: ruby -*-
  # vi: set ft=ruby :
  Vagrant.configure("2") do |config|
    config.vm.box = "singularityware/singularity-2.4"
    config.vm.synced_folder "${buildFolder.absolutePath}", "${buildFolder.absolutePath}"
  end
""".trimIndent()

const val VAGRANTFILE_NAME = "Vagrantfile"