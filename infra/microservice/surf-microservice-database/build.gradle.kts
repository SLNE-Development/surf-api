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

val surfDatabaseR2dbc = "dev.slne.surf:surf-database-r2dbc:2.+"
val surfNatsVersion = "1.+"

dependencies {
    api(projects.surfMicroserviceApi)
    compileOnly(surfDatabaseR2dbc)
    // Optional at runtime: the pool-sized NATS handler limits apply only when a microservice ships surf-nats.
    compileOnly("dev.slne.surf.nats:surf-nats-core:$surfNatsVersion")

    testImplementation(surfDatabaseR2dbc)
}
