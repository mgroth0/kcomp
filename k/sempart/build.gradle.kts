modtype = NO_NATIVE
apis(

)
implementations(

)
dependencies {
  commonMainImplementation(libs.kotlinx.serialization.json)
}

plugins {
  kotlin("plugin.serialization")
}

tasks {

}