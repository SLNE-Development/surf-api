plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientCommon)
}