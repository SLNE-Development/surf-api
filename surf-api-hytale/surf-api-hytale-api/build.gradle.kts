plugins {
    `core-convention`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi(libs.hytale.server)
}

description = "surf-api-hytale-api"
