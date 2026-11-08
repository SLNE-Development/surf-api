plugins {
    `vendor-bundle-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.spark)
    // Not bundled, see vendor-bundle-convention
    implementation(libs.asm)
}

description = "surf-api-minestom-spark-impl"

// Consumers of the Gradle plugin may still resolve the name this module had before the
// restructure, so the old coordinate keeps resolving and points here.
publishRelocationFrom("surf-api-minestom-server-spark")
