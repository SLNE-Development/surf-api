plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiSpringApi)
    api(projects.surfApiAnnotations)
}

description = "surf-api-event-api"
