plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

description = "surf-api-minestom-signed-chat-impl"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-signed-chat")
