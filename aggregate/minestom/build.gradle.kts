// surf-api-minestom is the coordinate downstream projects use. It carries no code of its own: the contracts live in surf-api-minestom-api and reach a consumer transitively through `api`.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiMinestomApi)
}

description = "surf-api-minestom"
