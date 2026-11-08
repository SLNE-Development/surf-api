plugins {
    id("surf.module-impl")
    `vendor-bundle-convention`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.luckperms)
}

description = "surf-api-minestom-luckperms-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-luckperms")
