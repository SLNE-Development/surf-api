plugins {
    `core-convention`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-server"))
    api(project(":surf-api-hytale:surf-api-hytale-api"))
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.fastutil)
    api(libs.flogger)
    api(libs.commons.math4.core)
}

tasks.withType<ProcessResources> {
    filesMatching("manifest.json") {
        expand(
            "version" to project.version,
            "id" to project.name,
            "name" to project.name
        )
    }
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("it.unimi.dsi.fastutil", "$relocationPrefix.fastutil")
    }
}

description = "surf-api-hytale-server"