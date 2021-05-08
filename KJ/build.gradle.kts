import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.internal.JavaJarExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
//import matt.groovyland.tomlVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val fxVersion = tomlVersion("fx") // .resolve()
val JIGSAW: Boolean by rootProject.extra
val thelibs = libs
val theMatt = projects
val shadowGradle = projectDir.resolve("shadow.gradle")

val abstractProjects = listOf<String>()
val mylibs = projectDir.resolve("libs.txt").readText().lines().filter { it.isNotBlank() }.map { it.trim() }
val applibs = projectDir.resolve("applibs.txt").readText().lines().filter { it.isNotBlank() }.map { it.trim() }
val clapps = projectDir.resolve("clapps.txt").readText().lines().filter { it.isNotBlank() }.map { it.trim() }


val guiapps = subprojects.map { it.path }.filter {
  it !in abstractProjects
  && it !in mylibs
  && it !in applibs
  && it !in clapps
}




subprojects sub@{
  val sp = this
  val spname = projectDir.name
  val sppath = path
  println("path example:${sppath}")

  val isAbstract = sppath in abstractProjects
  val isGuiApp = sppath in guiapps
  val isCLApp = sppath in clapps
  val isExecutable = isGuiApp || isCLApp
  val isAppLib = sppath in applibs
  val isBaseLib = sppath in mylibs
  val isAnyLib = isAppLib || isBaseLib

  if (isAbstract) return@sub

  repositories {
	mavenCentral()
	maven(url = "https://jitpack.io")
	/*mavenLocal() // for my reflections*/
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
	  listOf("base", "controls", "graphics", "web", "media").forEach {
		substitute(module("org.openjfx:javafx-$it"))
			.using(module("org.openjfx:javafx-$it:${fxVersion}"))
			.withClassifier("mac")
	  }
	}
  }

  if (isBaseLib) return@sub
  if (isAnyLib) return@sub
  require(isExecutable)

  if (isCLApp) {
	tasks.withType<JavaExec> {
	  standardInput = System.`in`
	}
	tasks.withType<JavaJarExec> {
	  //	  standardInput = System.`in` // TODO: test
	}
  }

  apply<ApplicationPlugin>()
  apply<ShadowPlugin>()

  val mainPackage = "matt.${path.replace(":KJ:","").replace(":",".").toLowerCase()}"


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

  configure<JavaApplication> {
	if (JIGSAW) mainModule.set(mainPackage)
	mainClass.set("${mainPackage}.${spname.capitalize()}MainKt")
  }
  apply(from = shadowGradle)
  tasks.withType<JavaExec> {
	enableAssertions = true
  }
  tasks.withType<JavaJarExec> {
	enableAssertions = true
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



