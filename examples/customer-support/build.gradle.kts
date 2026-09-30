plugins {
    id("jevface.example-conventions")
    id("org.springframework.boot")
}

description = "Example: a Spring Boot help desk that routes tickets with a typed Jev agent."

dependencies {
    implementation(project(":jevface-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation(project(":jevface-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
