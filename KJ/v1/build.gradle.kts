dependencies {
    implementation(projects.kj.gui)
    implementation(libs.bundles.multik.full)
    /*implementation(libs.aparapi)*/
    println("testing if newMac")
    if (isNewMac) {
        println("ok this is working...")
        implementation(
                files(
                        "/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi-natives.jar",
                        "/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi.jar",
                        "/Users/matthewgroth/registered/kcomp/KJ/jar/bcel-6.0.jar"
                )
        )
    } else if (isMac) {
        implementation(
                files(
                        "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi-natives.jar",
                        "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi.jar",
                        "/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/bcel-6.0.jar"
                )
        )
    } else {
        implementation(libs.aparapi)
    }
    implementation(libs.commons.math)
    implementation(libs.chartFX)
    implementation(libs.apfloat)
    implementation(libs.fx.swing)
    implementation(projects.kj.remote)
}