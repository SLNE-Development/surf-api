@file:OptIn(ExperimentalAbiValidation::class)

import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.core")
}

surfCoreApi {
    withApiValidation()
}

kotlin {
    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.nats.InternalNatsApi")
            }
        }
    }
}

publishing {
    repositories {
        slneReleases()
    }
}
