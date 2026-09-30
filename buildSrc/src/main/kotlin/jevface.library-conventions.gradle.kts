// Conventions for publishable library modules (publishToMavenLocal works today; add a remote repo later).
plugins {
    id("jevface.java-conventions")
    `java-library`
    `maven-publish`
}

val libs = versionCatalogs.named("libs")
val repositoryUrl = providers.gradleProperty("jevface.repositoryUrl")

java {
    // Libraries target Java 21 so consumers are not forced onto the newest JDK.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

dependencies {
    "api"(libs.findLibrary("jspecify").get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            // Publish concrete versions instead of BOM-managed (versionless) declarations.
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }
            pom {
                name = project.name
                description = project.description
                if (repositoryUrl.isPresent) {
                    url = repositoryUrl
                    scm {
                        url = repositoryUrl
                        connection = repositoryUrl.map { "scm:git:$it.git" }
                    }
                }
                developers {
                    developer {
                        name = "Rudolf Brisuda"
                    }
                }
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
            }
        }
    }
}
