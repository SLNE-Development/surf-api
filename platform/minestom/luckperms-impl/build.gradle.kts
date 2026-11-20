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

publishRelocationFrom("surf-api-minestom-server-luckperms")
