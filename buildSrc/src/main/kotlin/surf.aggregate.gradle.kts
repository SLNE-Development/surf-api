/**
 * Convention for a module that only groups other modules for consumers.
 *
 * An aggregate carries no sources of its own. It exposes the API modules of a platform through
 * `api` so they reach a consumer's compile classpath, and collects the matching implementations in
 * [SURF_RUNTIME], which feeds `runtimeOnly` and the shadow jar without ever becoming visible to a
 * consumer at compile time.
 *
 * The source check fails the build rather than ignoring stray sources: an aggregate that silently
 * compiles code would publish classes nobody expects there.
 */

plugins {
    id("core-convention")
}

val surfRuntime = configurations.register(SURF_RUNTIME) {
    description = "Implementation modules embedded into this aggregate's shadow jar."
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.named("runtimeOnly") {
    extendsFrom(surfRuntime.get())
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(surfRuntime.get())
}

afterEvaluate {
    val sources = the<SourceSetContainer>()
        .flatMap { sourceSet -> sourceSet.allSource.srcDirs }
        .filter { it.isDirectory && it.listFiles()?.isNotEmpty() == true }

    require(sources.isEmpty()) {
        "${project.path} applies surf.aggregate but carries sources: " +
                sources.joinToString { it.relativeTo(projectDir).path } +
                ". An aggregate only declares dependencies."
    }
}
