plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.nats")
}

dependencies {
    implementation(projects.surfNatsTest.surfNatsTestCommon)

    testImplementation(libs.testcontainers.nats)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.mariadb.jdbc)
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()

    // The end-to-end test starts the shadow jar exactly as the Docker image does.
    val microserviceJar = tasks.shadowJar.flatMap { it.archiveFile }
    inputs.file(microserviceJar).withPropertyName("microserviceJar")
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dsurf.microservice.jar=${microserviceJar.get().asFile.absolutePath}")
    }
}
