modtype = CLAPP

dependencies {
  implementation(projects.kj.exec)
  api(libs.expect)
  api(libs.jsch)
  implementation(projects.kj.async)
}