plugins {
    id("dev.slne.surf.surfapi.gradle.standalone")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiMicroservice)
    api(projects.surfMicroserviceCore)
    api(libs.spark.standalone.agent)
    runtimeOnly(libs.bundles.log4j)
}