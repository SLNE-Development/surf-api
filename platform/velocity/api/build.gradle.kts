plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    api(projects.surfApiConfigApi)
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
    compileOnlyApi(libs.commandapi.velocity.kotlin)
}

description = "surf-api-velocity-api"
