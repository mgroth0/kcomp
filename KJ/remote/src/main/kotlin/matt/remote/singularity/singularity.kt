package matt.remote.singularity

const val SINGULARITY_FILE_NAME = "Singularity"
const val SINGULARITY_SIMG_NAME = "kcomp.simg"

/*/Users/matt/Desktop/registered/todo/science/dnn/Singularity*/
data class SingularityRecipe(
  val java: Boolean = true
) {
  companion object {
	const val GRADLE_VERSION = "7.0.2"
  }

  val text = """
  Bootstrap: docker
From: ubuntu:20.04

%runscript
    echo "The runscript is the containers default runtime command!"
    exec echo "exec in the runscript replaces the current process!"

%labels
   AUTHOR mjgroth@mit.edu

%post
    echo "The post section is where you can install, and configure your container."
    apt update
    apt full-upgrade -y
    apt autoremove
	${
	if (java) """
	apt install software-properties-common -y
    add-apt-repository ppa:openjdk-r/ppa  
	apt update
	apt install openjdk-16-jdk -y
	apt install wget -y
	wget https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip -P /tmp
	apt install unzip -y
	unzip -d /opt/gradle /tmp/gradle-$GRADLE_VERSION-bin.zip
	
    echo "done with post-build"
	  """.trimIndent() else ""
  }
""".trimIndent()
}