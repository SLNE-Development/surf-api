plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientVelocity)
    api(projects.surfMicroserviceClient.surfMicroserviceClientCommon)
}