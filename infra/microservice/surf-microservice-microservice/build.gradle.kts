plugins {
    id("dev.slne.surf.surfapi.gradle.standalone")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiMicroservice)
    api(projects.surfMicroserviceCore)
    runtimeOnly(libs.bundles.log4j)
}