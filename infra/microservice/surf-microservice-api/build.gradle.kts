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
