plugins {
    id("dev.slne.surf.api.gradle.standalone")
}

repositories {
    maven("https://repo.slne.dev/repository/maven-public/")
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiMicroservice)
    api(projects.surfMicroserviceCore)
    api(libs.spark.standalone.agent) {
        exclude("org.slf4j")
    }
    runtimeOnly(libs.bundles.log4j)
}