modtype = JS_CLIENT

implementations(
  ":k:kjs".auto(),
  ":k:sempart".auto(),
)
plugins {
  kotlin("plugin.serialization")
}
dependencies {
  implementation(kotlin("reflect"))
}
