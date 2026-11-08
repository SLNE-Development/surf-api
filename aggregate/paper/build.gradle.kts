// surf-api-paper is the coordinate downstream projects use. It carries no code of its own: the contracts live in surf-api-paper-api and reach a consumer transitively through `api`.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiPaperApi)
}

description = "surf-api-paper"
