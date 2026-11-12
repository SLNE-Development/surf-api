plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    // The configuration domain is no longer re-exported by surf-api-core-api, so a consumer of
    // surf-api-velocity reaches the config contracts through here.
    api(projects.surfApiConfigApi)
    api(libs.mccoroutine.velocity.api)
    api(libs.mccoroutine.velocity.core)
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
    api(libs.commandapi.velocity.kotlin)
}

description = "surf-api-velocity-api"
