plugins {
    `vendor-bundle-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.luckperms)
}

description = "surf-api-minestom-server-luckperms"
