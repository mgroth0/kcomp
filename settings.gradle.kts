import matt.kbuild.settings.applySettings

//pluginManagement {
//
//  val props = java.util.Properties().apply {
//	load(
//	  buildscript.sourceFile!!.parentFile.resolve("gradle.properties").reader()
//	)
//  }
//
//
//  plugins {
//	id("com.github.johnrengelman.shadow").version(props["shadowPluginVersion"]!!.toString())
//	id("com.gradle.enterprise").version(props["gradleEnterprisePluginVersion"]!!.toString())
//  }
//}

buildscript {


  val PARTIAL_BOOTSTRAP =
	false /*should i just always keep this true now that I always will have a kbuild jar anyway for OpenMind?*/
  val NUM_BACK = 0


  repositories {
	mavenLocal()
	mavenCentral()
	google()
	gradlePluginPortal()
	//	maven {
	//	  url = uri("https://plugins.gradle.org/m2/") /*needed for com.faire.gradle.analyze later*/
	//	}
	maven(
	  url = "https://s01.oss.sonatype.org/content/repositories/releases/"
	)
  }
  val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

  configurations.all {
	attributes {
	  attribute(androidAttribute, false)
	}
  }
  dependencies {


	val props = java.util.Properties().apply {
	  load(
		sourceFile!!.parentFile.resolve("gradle.properties").reader()
		//		this@applySettings.buildscript.sourceFile!!.parentFile.resolve("gradle.properties").reader()
	  )
	}

	val osName = System.getProperty("os.name")

	val userHomeFolder = File(System.getProperty("user.home"))
	val registeredFolder = userHomeFolder.resolve("registered")


	val stupidKtVersion = /*sourceFile!!.parentFile.resolve("RootFiles")*/
	  (
		  registeredFolder.resolve("common").resolve("libs.versions.toml").takeIf { it.exists() }?.bufferedReader()
			?: java.net.URI(
			  "https://raw.githubusercontent.com/mgroth0/common/master/libs.versions.toml"
			).toURL().openStream().bufferedReader()
		  )
		.lines()
		.filter { "kotlin" in it }
		.findFirst()
		.get()
		.substringAfter("kotlin")
		.substringAfter('"')
		.substringBefore('"')

	classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$stupidKtVersion")
	classpath("org.jetbrains.kotlin:kotlin-serialization:$stupidKtVersion")
	classpath("com.gradle.enterprise:com.gradle.enterprise.gradle.plugin:${props["gradleEnterprisePluginVersion"]!!}")
	classpath("gradle.plugin.com.github.johnrengelman:shadow:${props["shadowPluginVersion"]!!}")
	classpath("com.dorongold.plugins:task-tree:${props["taskTreeVersion"]!!}")

	val registeredDir = userHomeFolder.resolve("registered")
	val kbuildDir = registeredDir.resolve("kbuild")

	if (osName == "Windows 11") {
	  classpath(files("Y:\\kbuild.jar"))
	} else if (/*osName == "Linux" ||*/ PARTIAL_BOOTSTRAP) {
	  classpath(files(registeredFolder.resolve("kbuild.jar")))
	  /*} else if (NUM_BACK == 0) classpath("matt.flow:kbuild:+")*/
	} else if (NUM_BACK == 0) classpath(fileTree(registeredDir.resolve("bin/dist/kbuild/lib")))
	else {

	  /*	  val recentVersion = userHomeFolder.resolve(".m2/repository/matt/flow/kbuild").list()!!.mapNotNull {
			  it.toLongOrNull()
			}.sorted().reversed()[NUM_BACK]
			classpath("matt.flow:kbuild:$recentVersion")*/


	  val recentVersion = kbuildDir.list()!!.mapNotNull {
		it.toLongOrNull()
	  }.sorted().reversed()[NUM_BACK]
	  classpath(fileTree(kbuildDir.resolve("$recentVersion")))


	}
  }
}


//plugins {
//  id("com.github.johnrengelman.shadow")
//}
applySettings()
