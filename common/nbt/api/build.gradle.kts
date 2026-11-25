plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiAnnotations)

    compileOnlyApi(libs.kotlin.stdlib)
    compileOnlyApi(libs.adventure.nbt)
}

description = "surf-api-nbt-api"
