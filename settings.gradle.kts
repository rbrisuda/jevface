pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "jevface"

include(
    "jevface-core",
    "jevface-typesafe",
    "jevface-test",
    "jevface-spring-boot-starter",
    "examples:customer-support",
    "examples:shopping-agent",
)
