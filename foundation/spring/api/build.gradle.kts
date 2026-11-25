plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiAnnotations)
    compileOnlyApi(platform(libs.spring.framework.bom))
    compileOnlyApi(libs.spring.context)
    compileOnlyApi(libs.kotlin.stdlib)
}

description = "surf-api-spring-api"
