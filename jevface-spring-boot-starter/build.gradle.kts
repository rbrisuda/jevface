plugins {
    id("jevface.library-conventions")
}

description = "Spring Boot auto-configuration for jevface: Jevface, the TypeSafe Jev engine and injectable JevClient<T> beans."

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":jevface-typesafe"))
    api(libs.spring.ai.starter.typesafe)
    api("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":jevface-test"))
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework:spring-test")
}
