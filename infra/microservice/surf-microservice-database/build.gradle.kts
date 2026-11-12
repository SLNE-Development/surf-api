@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.core")
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

dependencies {
    api(projects.surfApiMicroservice.surfApiMicroserviceApi)
    compileOnly(shadowed(projects.surfApiDatabaseR2dbc))
    // Optional at runtime: the pool-sized NATS handler limits apply only when a microservice ships surf-nats.
    compileOnly(projects.surfApiNats.surfApiNatsCore)

    testImplementation(shadowed(projects.surfApiDatabaseR2dbc))
}
