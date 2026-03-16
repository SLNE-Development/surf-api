plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiRuntime.surfMicroserviceApiRuntimeVelocity)
}