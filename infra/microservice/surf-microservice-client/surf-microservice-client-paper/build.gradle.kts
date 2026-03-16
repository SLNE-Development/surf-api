plugins {
    id("dev.slne.surf.surfapi.gradle.paper-raw")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiClient.surfMicroserviceApiClientPaper)
    api(projects.surfMicroserviceClient.surfMicroserviceClientCommon)
}