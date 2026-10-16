plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    api(libs.terminal.console.appender)
    api(libs.log4j.core)
}

description = "surf-api-minestom-server-console"
