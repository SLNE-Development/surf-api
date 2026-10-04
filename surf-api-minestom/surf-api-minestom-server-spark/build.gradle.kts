plugins {
    `vendor-bundle-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    bundled(libs.minestom.spark)
}

description = "surf-api-minestom-server-spark"
