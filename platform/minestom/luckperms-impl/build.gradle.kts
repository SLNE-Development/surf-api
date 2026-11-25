plugins {
    id("surf.module-impl")
    `vendor-bundle-convention`
}

dependencies {
    api(projects.surfApiMinestomImpl)
    implementation(projects.surfApiCoreImpl)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.luckperms)

    testImplementation(libs.luckperms)
}

description = "surf-api-minestom-luckperms-impl"
