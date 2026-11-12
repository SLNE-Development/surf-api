@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    explicitApi()

    jvm()

    js {
        nodejs()
    }

    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.nats.InternalNatsApi")
            }
        }
    }

    sourceSets {
        jvmMain.dependencies {
            compileOnly(libs.surf.api.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)

            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
