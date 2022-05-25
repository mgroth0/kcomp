import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

val ktVersion: String by extra

val JIGSAW: Boolean by rootProject.extra



subprojects {

  val sp = this

  sp.apply<org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper>()
  sp.apply<JavaLibraryPlugin>()

  sp.configure<JavaPluginExtension> {
	modularity.inferModulePath.set(JIGSAW)
	val javaLangVersion = JavaLanguageVersion.of(tomlVersion("java"))
	val javaVersion = JavaVersion.toVersion(tomlVersion("java"))
	toolchain {
	  languageVersion.set(javaLangVersion)
	}
  }


  sp.configure<KotlinMultiplatformExtension> {

	jvm {


	  /*
		  withJava does not work when using gradle 7.0 + kotlin 1.4.* (for now?)
		  However, I absolutely don't want to downgrade gradle (and loose version catalogs / type safe project dependencies) or upgrade kotlin (and lose global kotlin helper functions in buildscripts). So conclusions: no java in here for now. But that should be fine! The only reason I needed java here was in order for Jigsaw to work, which A) I'm currently not using and B) a this point I should be absle to find a workaround and create an automatic module or whatever if I absolutely have to use jigsaw ... at the bottom of this buildscript it looks like I did something like this with --patch-module
	  */
	  // NOTE:	withJava()

	  //	compilations.all {
	  //	  kotlinOptions.mventionKotlinJvmOptions()
	  //	}


	}

	sourceSets {
	  all {
		/*https://kotlinlang.org/docs/opt-in-requirements.html#module-wide-opt-in*/
		//	  languageSettings.optIn("org.mylibrary.OptInAnnotation")
		//	  languageSettings.useExperimentalAnnotation("org.mylibrary.OptInAnnotation")
	  }
	}
  }

  if (JIGSAW) {
	tasks.withType<JavaCompile> {
	  val reason = "DEBUG IDEA"
	  doFirst {
		err("obviously this wont work as is")
		@Suppress("UNREACHABLE_CODE")
		options.compilerArgs = listOf(
		  "--patch-module", "moduleName=matt.klib"
		)
	  }
	}
  }
  //repositories {
  //  mavenCentral()
  //}


}