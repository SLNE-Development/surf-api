plugins {
    id("dev.slne.surf.surfapi.gradle.standalone")
}

dependencies {
    api(projects.surfMicroserviceCore.surfMicroserviceCoreMicroservice)
    runtimeOnly(libs.bundles.log4j)
}