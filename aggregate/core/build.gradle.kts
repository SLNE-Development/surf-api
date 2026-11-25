plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiCoreApi)
    api(projects.surfApiConfigApi)
}

description = "surf-api-core"
