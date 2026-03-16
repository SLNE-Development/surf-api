plugins {
    id("dev.slne.surf.surfapi.gradle.velocity")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientCommon)
}