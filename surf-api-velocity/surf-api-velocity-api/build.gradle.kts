plugins {
    `core-convention`
//    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    api(libs.mccoroutine.velocity.api)
    api(libs.mccoroutine.velocity.core)
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
    api(libs.commandapi.velocity.kotlin)
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
    }
}

description = "surf-api-velocity-api"
