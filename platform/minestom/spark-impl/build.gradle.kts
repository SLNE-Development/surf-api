plugins {
    id("surf.module-impl")
    `vendor-bundle-convention`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.spark)
    // Not bundled, see vendor-bundle-convention
    implementation(libs.asm)
}

description = "surf-api-minestom-spark-impl"
