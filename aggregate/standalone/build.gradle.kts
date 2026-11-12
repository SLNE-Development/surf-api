// surf-api-standalone is the coordinate downstream projects use. It carries no code of its own: it
// exposes surf-api-standalone-impl, which re-exports the whole standalone surface -- and that is
// the point rather than a loose end. "Standalone" means surf-api in a plain JVM, so a consumer
// expects the surface, and the one public entry point is a bootstrap, which cannot be separated
// from its implementation: it runs before the container exists, so it cannot reach the
// implementation as a bean, and a service-loader seam for it would be a second one of exactly the
// kind the container rebuild removed.
plugins {
    id("surf.aggregate")
}

dependencies {
    api(projects.surfApiStandaloneImpl)
}

description = "surf-api-standalone"
