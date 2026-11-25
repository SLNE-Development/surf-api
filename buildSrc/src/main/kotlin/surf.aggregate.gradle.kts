/**
 * Convention for a module that only groups other modules for consumers.
 *
 * An aggregate carries no sources of its own. It exposes the modules of a platform through `api`,
 * and the runtime of a coordinate that runs on its own comes from the implementation module it
 * exports.
 *
 * The source check fails the build rather than ignoring stray sources: an aggregate that silently
 * compiles code would publish classes nobody expects there.
 */

plugins {
    id("core-convention")
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
