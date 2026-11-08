// surf-api-core is the coordinate downstream projects use. It carries no code of its own: the contracts live in surf-api-core-api and reach a consumer transitively through `api`.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiCoreApi)
}

description = "surf-api-core"
