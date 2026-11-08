// surf-api-velocity is the coordinate downstream projects use. It carries no code of its own: the contracts live in surf-api-velocity-api and reach a consumer transitively through `api`.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiVelocityApi)
}

description = "surf-api-velocity"
