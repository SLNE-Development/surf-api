plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

description = "surf-api-minestom-server-signed-chat"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}
