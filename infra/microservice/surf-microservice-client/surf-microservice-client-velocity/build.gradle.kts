plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientVelocity)
    api(projects.surfMicroserviceClient.surfMicroserviceClientCommon)
}