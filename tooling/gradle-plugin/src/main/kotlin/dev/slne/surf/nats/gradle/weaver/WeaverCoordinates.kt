package dev.slne.surf.nats.gradle.weaver

import org.gradle.api.GradleException

/**
 * Coordinate and repository name rules of the weaver registry.
 *
 * Mirrors the server's validation so that an invalid `weaver.publishing` block fails at configuration time
 * instead of at upload.
 */
internal object WeaverCoordinates {

    private val REPOSITORY = Regex("""^[a-z0-9][a-z0-9-]{0,62}$""")
    private val GROUP = Regex("""^[a-z0-9_]+(\.[a-z0-9_-]+)*$""")
    private val NAME = Regex("""^[a-z0-9][a-z0-9._-]{0,99}$""")
    private val VERSION = Regex("""^\d+(\.\d+){0,3}(-[0-9A-Za-z.-]+)?$""")
    private const val VERSION_MAX_LENGTH = 64

    /**
     * Returns one message per missing or invalid value.
     */
    fun problems(
        group: String?,
        name: String?,
        version: String?,
        repository: String?,
        repositories: List<String>,
    ): List<String> = buildList {
        if (group.isNullOrBlank()) {
            add("weaver.publishing.group is not set (defaults to project.group)")
        } else if (!GROUP.matches(group)) {
            add("weaver.publishing.group '$group' must be a lowercase reverse-DNS name such as dev.slne.surf.demo")
        }

        if (name.isNullOrBlank()) {
            add("weaver.publishing.name is not set (defaults to rootProject.name)")
        } else if (!NAME.matches(name)) {
            add("weaver.publishing.name '$name' must match ${NAME.pattern}")
        }

        if (version.isNullOrBlank()) {
            add("weaver.publishing.version is not set (defaults to project.version)")
        } else if (version.length > VERSION_MAX_LENGTH || !VERSION.matches(version)) {
            add(
                "weaver.publishing.version '$version' must have 1 to 4 numeric parts and an optional qualifier " +
                        "(1.4.0, 1.4.0-rc.1, 1.4.0-SNAPSHOT), at most $VERSION_MAX_LENGTH characters"
            )
        }

        if (repository.isNullOrBlank()) {
            add("weaver.publishing.repository is not set")
        } else if (!REPOSITORY.matches(repository)) {
            add("weaver.publishing.repository '$repository' must match ${REPOSITORY.pattern}")
        }

        for (lookup in repositories.filterNot(REPOSITORY::matches)) {
            add("weaver.repositories entry '$lookup' must match ${REPOSITORY.pattern}")
        }
    }

    /**
     * Returns the `group:name:version` of an external `weaver` dependency.
     *
     * @throws GradleException if the group or version is missing, as weaver itself requires both.
     */
    fun external(group: String?, name: String, version: String?): String {
        if (group.isNullOrBlank() || version.isNullOrBlank()) {
            throw GradleException("weaver dependency ${group.orEmpty()}:$name needs a group and a version (group:name:version).")
        }
        return "$group:$name:$version"
    }
}
