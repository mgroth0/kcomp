/*This file is currently hard-linked across 2 projects*/

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.internal.JavaJarExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import matt.kbuild.shell
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.System.currentTimeMillis
import kotlin.script.experimental.jvm.util.hasParentNamed

val fxVersion = tomlVersion("fx") // .resolve()
val JIGSAW: Boolean by rootProject.extra
val thelibs = libs
val theMatt = projects
val shadowGradle = projectDir.resolve("shadow.gradle")

val kjProj = this

subprojects sub@{
  val sp = this
  val spname = projectDir.name
  val sppath = path
  //  println("path example:${sppath}")

  val modtype = ModType.valueOf(projectDir.resolve("modtype.txt").readText().trim())
  val isExecutable = modtype in listOf(ModType.APP, ModType.CLAPP)
  val isAnyLib = modtype in listOf(ModType.LIB, ModType.APPLIB)

  if (modtype == ModType.ABSTRACT) return@sub

  if (!projectDir.resolve(".gitignore").exists()) {
	/*since I plan to convert most to git submodules anyway...*/
	/*must have leading slash here or packages called build wont be commited*/
	projectDir.resolve(".gitignore").writeText("/build/")
  }

  repositories {
	mavenCentral()
	maven(url = "https://jitpack.io")
	mavenLocal() // for my reflections
  }

  apply<JavaPlugin>()
  configure<JavaPluginExtension> {
	modularity.inferModulePath.set(JIGSAW)
	val javaLangVersion = JavaLanguageVersion.of(tomlVersion("java"))
	val javaVersion = JavaVersion.toVersion(tomlVersion("java"))
	toolchain {
	  languageVersion.set(javaLangVersion)
	}

	/*"The new Java toolchain feature cannot be used at the project level in combination with source and/or target compatibility"*/
	/*sourceCompatibility = javaVersion
	targetCompatibility = javaVersion*/

  }

  apply<KotlinPluginWrapper>()
  //  apply(serGradle)


  val ENABLE_GIT: Boolean by rootProject.extra

  if (ENABLE_GIT) {
	this.tasks.withType<KotlinCompile>() {
	  dependsOn(project(":KJ:kbuild").tasks["gitPullSubmodule"])
	}
  }


  //  apply(SerializationPluginContext)

  if (projectDir.resolve("src").resolve("test").exists()) {
	apply<matt.jbuild.test.ThisBuildIncludesTests>()
  }

  gradle.projectsEvaluated {
	sp.configurations.forEach { c ->
	  c.dependencies.withType<ProjectDependency>().forEach { dep ->
		val testName = if (dep.dependencyProject.projectDir.hasParentNamed("k")) "jvmTest" else "test"
		dep.dependencyProject.tasks.getByName(testName).let { t ->
		  sp.tasks.withType<KotlinCompile> {
			dependsOn(t)
		  }
		  sp.tasks.withType<JavaCompile> {
			dependsOn(t)
		  }
		}
	  }
	}
  }
  gradle.taskGraph.afterTask {}
  gradle.taskGraph.beforeTask { }
  gradle.taskGraph.addTaskExecutionGraphListener { }
  gradle.taskGraph.whenReady { }



  if (isAnyLib) {
	apply<JavaLibraryPlugin>()

	/*unlike with the kotlin multiplatform plugin, publications are not being added automatically here. no idea why. but adding it myself below is working*/
	/*actually, registering a gradle plugin might have saved me with kjlib!*/


	val isLang = (projectDir.parentFile.name == "kjlib" && projectDir.name == "lang")
	if (LAST_VERSION_TXT in projectDir.list()) {
	  sp.apply<MavenPublishPlugin>()

	  sp.afterEvaluate {
		if (!sp.pluginManager.hasPlugin("org.gradle.java-gradle-plugin")) {
		  sp.configure<PublishingExtension>() {
			this.publications {
			  //		  println("itrating kbuild pubs")
			  this.forEach {
				println("kbuild:pub:${it.name}")
			  }
			  this.register("mavenJava", MavenPublication::class) {
				this.from(components["java"])
			  }
			}
		  }
		}
	  }



	  println("sp.group:${sp.group},sp.name=${sp.name}")
	  sp.setupMavenTasks(
		compileKotlinJvmTaskName = "compileKotlin",
		jarTaskName = "jar"
	  )
	}
  }







  configurations.all {
	resolutionStrategy.dependencySubstitution {
	  listOf("base", "controls", "graphics", "web", "media", "swing").forEach {
		substitute(module("org.openjfx:javafx-$it"))
		  .using(module("org.openjfx:javafx-$it:${fxVersion}"))
		  .withClassifier(


			when (thisMachine) {
			  NEW_MAC -> "mac-aarch64"
			  OLD_MAC -> "mac"
			  else    -> "linux"
			}


			//			if (isNewMac) run {
			//			  //				println("isNewMac")
			//
			//			} else if (ismac) run {
			//			  //				println("isOldMac")
			//			  "mac"
			//			} else "linux"
		  )
	  }
	}
  }

  if (modtype == ModType.LIB) return@sub
  if (isAnyLib) return@sub
  require(isExecutable)

  if (modtype == ModType.CLAPP) {
	tasks.withType<JavaExec> {
	  standardInput = System.`in`
	}
	tasks.withType<JavaJarExec> {
	  //	  standardInput = System.`in` // TODO: test
	}
  }

  apply<ApplicationPlugin>()
  apply<ShadowPlugin>()

  val mainPackage = "matt.${path.replace(":KJ:", "").replace(":", ".").toLowerCase()}"


  val createAppNameResource by tasks.creating {
	val f = FixedFile(projectDir)["src"]["main"]["resources"]["matt"]["appname.txt"]
	this.onlyIf { !f.exists() || f.readText() != spname }
	doLast {
	  f.parentFile.mkdirs()
	  f.writeText(spname)
	}
  }
  tasks.withType<ProcessResources> {
	dependsOn(createAppNameResource)
  }

  val jvmRuntimeArgs = listOf(
	if (isNewMac) "-Xmx30g" else "-Xmx6g",
	/*"-agentpath:"*/ /*jProfiler*/
	"-Djava.library.path=/Users/matthewgroth/registered/kcomp/KJ/dylib"
  )

  configure<JavaApplication> {
	if (JIGSAW) mainModule.set(mainPackage)
	if (FixedFile(projectDir)["src"]["main"]["kotlin"].exists()) {
	  mainClass.set("${mainPackage}.${spname.capitalize()}MainKt")
	} else {
	  mainClass.set("${mainPackage}.${spname.capitalize()}Main")
	}


	/*getting deprecation warnings for this*/
	/*mainClassName = mainClass.get()*/


	this.applicationDefaultJvmArgs = jvmRuntimeArgs
  }


  tasks.withType<JavaExec> {
	enableAssertions = true
	/*this.args = jvmRuntimeArgs*/
	this.jvmArgs = jvmRuntimeArgs
  }




  tasks.withType<JavaJarExec> {
	enableAssertions = true
	/*this.args = jvmRuntimeArgs*/
	this.jvmArgs = jvmRuntimeArgs
  }
  configure<ShadowExtension>() {
  }
  tasks.withType<ShadowJar> {


	/*this.minimize()*/
	/*seems maybe a bit faster (like 10-20% faster, but just maybe), but its not worth it. Right away had an error because jfx wasn't included
	* yes you can exclude/include things or whatever. But I think if I properly modularize this shouldn't be a big issue anyway
	* and I don't want the added layer of complexity of not being sure that something will be included in the shadowJar
	* because now if it works in the gradle run task I pretty much know it will work as a shadow. I dont want to lose that.*/


	this.isZip64 = true
	doLast {
	  if (this.didWork) {
		val shadowJar = buildDir.resolve("libs")
		  .listFiles()!!
		  .first { it.name.contains("-all.jar") }
		val dest = rootDir
		  .resolve("bin/jar/")
		  .apply { mkdirs() }
		  .resolve(shadowJar.name)

		//		val t1 = currentTimeMillis()

		/*what a SHAM. This can take over 10 times as long as cp*/
		/*shadowJar.copyTo(dest, overwrite = true)*/

		//		val t2 = currentTimeMillis()
		shell("cp", "-rf", shadowJar.absolutePath, dest.absolutePath)
		//		val t3 = currentTimeMillis()
		//		println("$sp :copyTo took ${(t2 - t1)/1000.0} secs")
		//		println("$sp :cp took ${(t3 - t2)/1000.0} secs")
	  }
	}
  }
}



