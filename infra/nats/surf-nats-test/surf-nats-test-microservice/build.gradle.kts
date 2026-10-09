plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.microservice")
}

surfMicroservice {
    withMicroserviceApi()
}

dependencies {
    implementation(projects.surfNatsStandalone)
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}
