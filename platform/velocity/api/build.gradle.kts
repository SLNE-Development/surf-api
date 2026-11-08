plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    api(libs.mccoroutine.velocity.api)
    api(libs.mccoroutine.velocity.core)
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
    api(libs.commandapi.velocity.kotlin)
}

description = "surf-api-velocity-api"
