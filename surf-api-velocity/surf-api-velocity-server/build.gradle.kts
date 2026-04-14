plugins {
    `core-convention`
    kotlin("kapt")
}

dependencies {
    api(projects.surfApiCore.surfApiCoreServer)
    api(projects.surfApiVelocity.surfApiVelocity)
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.fastutil)
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    api(libs.aide.reflection)
    runtimeOnly(libs.flogger.slf4j.backend)
    kapt(libs.velocity.api)
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("it.unimi.dsi.fastutil", "$relocationPrefix.fastutil")

        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*"))
        }
    }
}

description = "surf-api-velocity-server"

kapt {
    keepJavacAnnotationProcessors = true
}