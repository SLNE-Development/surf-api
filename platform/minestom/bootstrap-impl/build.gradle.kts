plugins {
    id("surf.module-impl")
}

dependencies {
    implementation(libs.gremlin.runtime)
}

description = "surf-api-minestom-bootstrap-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-bootstrap")
