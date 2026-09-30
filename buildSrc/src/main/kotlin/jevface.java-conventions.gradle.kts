import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// Shared settings for every Java module: Java release, JSpecify + NullAway, JUnit 5.
plugins {
    java
    id("net.ltgt.errorprone")
}

val libs = versionCatalogs.named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    "errorprone"(libs.findLibrary("errorprone-core").get())
    "errorprone"(libs.findLibrary("nullaway").get())

    testImplementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Parameter names are kept so error messages and Spring binding can use them.
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        if (name.contains("Test", ignoreCase = true)) {
            disable("NullAway")
        } else {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:OnlyNullMarked", "true")
            option("NullAway:JSpecifyMode", "true")
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        // Tests tagged "live" call the real Jev API and only run via the liveTest task.
        excludeTags("live")
    }
}

tasks.register<Test>("liveTest") {
    description = "Runs tests against the real TypeSafe Jev API (needs TYPESAFE_API_KEY)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("live") }
}
