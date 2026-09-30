plugins {
    id("jevface.library-conventions")
}

description = "JudgmentEngine backed by TypeSafe's Jev API via the Spring AI Community TypeSafe Java SDK."

dependencies {
    api(project(":jevface-core"))
    api(libs.typesafe.java.sdk)

    testImplementation("org.springframework:spring-test")
    testImplementation("org.hamcrest:hamcrest")
    testRuntimeOnly("org.skyscreamer:jsonassert")
}
