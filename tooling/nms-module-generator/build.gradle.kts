plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

/**
 * Copies the runtime `NmsVersion` enum into this build, so the generator and the Paper runtime
 * share one list of supported versions.
 */
val syncNmsVersion = tasks.register<Sync>("syncNmsVersion") {
    from("../../foundation/internal/src/main/kotlin") {
        include("dev/slne/surf/api/shared/internal/nms/NmsVersion.kt")
    }
    into(layout.buildDirectory.dir("generated/shared-nms-version/kotlin"))
}

kotlin.sourceSets.main {
    kotlin.srcDir(syncNmsVersion)
}

gradlePlugin {
    plugins {
        create("nms-module-generator") {
            id = "dev.slne.surf.api.generator.nms-module-generator"
            implementationClass = "dev.slne.surf.api.generator.nms.GenerateNmsModulePlugin"
        }
    }
}
