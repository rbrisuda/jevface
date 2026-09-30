// Conventions for example applications (not published).
plugins {
    id("jevface.java-conventions")
}

val libs = versionCatalogs.named("libs")

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
    implementation(libs.findLibrary("jspecify").get())
}
