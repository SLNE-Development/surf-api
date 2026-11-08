// surf-api-standalone is the coordinate downstream projects use. It carries no code of its own: it exposes surf-api-standalone-impl, which still re-exports the whole standalone surface; splitting that is a later step.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiStandaloneImpl)
}

description = "surf-api-standalone"
