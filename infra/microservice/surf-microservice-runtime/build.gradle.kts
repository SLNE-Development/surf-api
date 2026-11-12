plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("com.github.gmazzo.buildconfig") version "6.0.9"
}

repositories {
    maven("https://repo.slne.dev/repository/maven-public/")
}

dependencies {
    api(projects.surfMicroserviceApi)
    api(libs.spark.standalone.agent) {
        exclude("org.slf4j")
    }
    implementation(libs.log4j.api)
    runtimeOnly(libs.bundles.log4j)
}

buildConfig {
    packageName("dev.slne.surf.microservice.runtime")
    className("MicroserviceRuntimeBuildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("VERSION", provider { project.version.toString() })
}
