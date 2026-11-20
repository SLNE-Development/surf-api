plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiSpringApi)
    api(projects.surfApiAnnotations)

    api(libs.adventure.nbt)
}

description = "surf-api-nbt-api"
