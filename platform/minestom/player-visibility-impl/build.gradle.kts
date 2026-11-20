plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
}

description = "surf-api-minestom-player-visibility-impl"

publishRelocationFrom("surf-api-minestom-server-player-visibility")
