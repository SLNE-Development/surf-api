// surf-api-core is the coordinate downstream projects use. It carries no code of its own: the contracts live in surf-api-core-api and reach a consumer transitively through `api`.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiCoreApi)
    // The configuration domain was cut out of surf-api-core-api and is not re-exported by it, so
    // the re-export happens here instead: a consumer of surf-api-core keeps resolving the config
    // contracts, the constraint annotations and the helper types unchanged.
    api(projects.surfApiConfigApi)
}

description = "surf-api-core"
