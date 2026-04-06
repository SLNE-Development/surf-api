plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiCore.surfApiCore)
    api(libs.mccoroutine.velocity.api)
    api(libs.mccoroutine.velocity.core)
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
    api(libs.commandapi.velocity.kotlin)
}

description = "surf-api-velocity"
