plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientCommon)
}