// surf-api-standalone is the coordinate downstream projects use. It carries no code of its own and
// re-exports surf-api-standalone-impl, the whole standalone surface. The one public entry point is
// SurfApiStandaloneBootstrap, which creates surf-api's root Spring context and therefore cannot be
// looked up as a bean of that context or separated from its implementation.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiStandaloneImpl)
}

description = "surf-api-standalone"
