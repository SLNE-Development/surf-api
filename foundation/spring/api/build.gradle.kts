plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiAnnotations)
    api(platform(libs.spring.framework.bom))
    api(libs.spring.context)
    api(libs.kotlin.stdlib)
}

description = "surf-api-spring-api"
