@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.core")
    `api-validation`
}

kotlin {
    explicitApi()

    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.microservice.api.InternalMicroserviceApi")
            }
        }
    }
}

surfCoreApi {
    allowInternalApi("dev.slne.surf.api.shared.api.util.InternalSurfApi")
}

dependencies {
    api(projects.surfApiMicroservice.surfApiMicroserviceApi)
    compileOnly(shadowed(projects.surfApiDatabaseR2dbc))
    compileOnly(libs.spring.context)
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.health)
    compileOnly(projects.surfApiNats.surfApiNatsCore)

    testImplementation(shadowed(projects.surfApiDatabaseR2dbc))
    testImplementation(libs.spring.context)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.spring.boot.health)
}
