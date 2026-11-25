@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.standalone")
    `api-validation`
    id("com.github.gmazzo.buildconfig") version "6.0.9"
}

kotlin {
    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.microservice.api.InternalMicroserviceApi")
            }
        }
    }
}

surfStandaloneApi {
    allowInternalApi("dev.slne.surf.microservice.api.InternalMicroserviceApi")
}

repositories {
    maven("https://repo.slne.dev/repository/maven-public/")
}

dependencies {
    api(projects.surfApiMicroservice.surfApiMicroserviceApi)
    api(libs.spark.standalone.agent) {
        exclude("org.slf4j")
    }
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.health)
    implementation(libs.log4j.api)
    runtimeOnly(libs.bundles.log4j)
}

buildConfig {
    packageName("dev.slne.surf.microservice.runtime")
    className("MicroserviceRuntimeBuildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("VERSION", provider { project.version.toString() })
}
