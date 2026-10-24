plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
}

description = "surf-api-minestom-server-player-visibility"
