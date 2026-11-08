// surf-api-minestom-server is the coordinate downstream projects use. It carries no code of its own: this coordinate is used to build a runnable server, so it exposes surf-api-minestom-impl the way the module of this name did before.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiMinestomImpl)
}

description = "surf-api-minestom-server"
