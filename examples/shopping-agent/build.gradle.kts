plugins {
    id("jevface.example-conventions")
    application
}

description = "Example: plain Java (no Spring Boot) product review analysis with a typed Jev agent."

dependencies {
    implementation(project(":jevface-typesafe"))
    runtimeOnly("org.slf4j:slf4j-simple")

    testImplementation(project(":jevface-test"))
}

application {
    mainClass = "io.github.rbrisuda.jevface.examples.shopping.ReviewInsightsMain"
}
