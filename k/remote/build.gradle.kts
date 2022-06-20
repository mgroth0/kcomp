modtype = CLAPP

dependencies {
  implementation(projects.k.exec)
  api(libs.expect)
  api(libs.jsch)
  implementation(projects.k.async)
}