plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    api(libs.terminal.console.appender)
    api(libs.log4j.core)
}

description = "surf-api-minestom-console-impl"
