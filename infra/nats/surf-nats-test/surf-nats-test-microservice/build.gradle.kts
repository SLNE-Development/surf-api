plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.microservice")
    id("dev.slne.surf.nats")
}

surfMicroservice {
    withMicroserviceApi()
}

dependencies {
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}
