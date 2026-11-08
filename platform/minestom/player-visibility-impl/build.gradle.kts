plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
}

description = "surf-api-minestom-player-visibility-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-player-visibility")
