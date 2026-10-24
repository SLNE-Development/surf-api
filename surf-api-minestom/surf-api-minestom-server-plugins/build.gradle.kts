plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(testFixtures(projects.surfApiMinestom.surfApiMinestomServer))
}

description = "surf-api-minestom-server-plugins"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}
