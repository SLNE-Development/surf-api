plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    api(libs.terminal.console.appender)
    api(libs.log4j.core)
}

description = "surf-api-minestom-console-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-console")
