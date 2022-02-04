/*This file is currently hard-linked across 2 projects*/

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.internal.JavaJarExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val fxVersion = tomlVersion("fx") // .resolve()
val JIGSAW: Boolean by rootProject.extra
val thelibs = libs
val theMatt = projects
val shadowGradle = projectDir.resolve("shadow.gradle")


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
  }

  apply<KotlinPluginWrapper>()

  if (projectDir.resolve("src").resolve("test").exists()) {
	apply<matt.jbuild.test.ThisBuildIncludesTests>()
  }

  gradle.projectsEvaluated {
	sp.configurations.forEach { c ->
	  c.dependencies.withType<ProjectDependency>().forEach { dep ->
		dep.dependencyProject.tasks.getByName("test").let { t ->
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
  }




	configurations.all {
		resolutionStrategy.dependencySubstitution {
			listOf("base", "controls", "graphics", "web", "media", "swing").forEach {
				substitute(module("org.openjfx:javafx-$it"))
						.using(module("org.openjfx:javafx-$it:${fxVersion}"))
						.withClassifier(if (isNewMac) run {
//				println("isNewMac")
							"mac-aarch64"
						}else if (isMac) run{
//				println("isOldMac")
							"mac"} else "linux")
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
	val f = FixedFile(projectDir)["src"]["main"][
		"resources"
	]["matt"]["appname.txt"]
	this.outputs.file(f.absolutePath)
	doLast {
	  if (!f.exists()) {
		f.writeText(spname)
	  }
	}
  }
  tasks.withType<ProcessResources> {
	dependsOn(createAppNameResource)
  }

  val jvmRuntimeArgs = listOf(
	"-Xmx6g"
  )

  configure<JavaApplication> {
	if (JIGSAW) mainModule.set(mainPackage)
	mainClass.set("${mainPackage}.${spname.capitalize()}MainKt")
	this.applicationDefaultJvmArgs = jvmRuntimeArgs
  }
  apply(from = shadowGradle)

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
  tasks.withType<ShadowJar> {
	doLast {
	  val sjar = buildDir.resolve("libs")
		  .listFiles()!!
		  .first { it.name.contains("-all.jar") }
	  print("copying ${sjar.name}... ")
	  sjar
		  .copyTo(
			rootDir
				.resolve("bin/jar/")
				.apply { mkdirs() }
				.resolve(sjar.name),
			overwrite = true
		  )
	  println("done")
	}
  }
}



