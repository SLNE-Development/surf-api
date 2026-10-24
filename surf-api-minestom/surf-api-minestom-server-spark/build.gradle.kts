plugins {
    `vendor-bundle-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.spark)
    // Not bundled, see vendor-bundle-convention
    implementation(libs.asm)
}

description = "surf-api-minestom-server-spark"
