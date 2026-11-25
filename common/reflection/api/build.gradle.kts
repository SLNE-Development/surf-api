plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)

    testImplementation(libs.kotlin.stdlib)
}

description = "surf-api-reflection-api"
