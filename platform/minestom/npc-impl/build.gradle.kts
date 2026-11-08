plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    api(libs.minestom.npc) { exclude(group = "net.minestom") }

    compileOnly(libs.sponge.mixin)
    compileOnly(libs.mixinextras)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(testFixtures(projects.surfApiMinestomImpl))
}

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}

description = "surf-api-minestom-npc-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-npc")
