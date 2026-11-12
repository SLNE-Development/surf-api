/**
 * Version alignment for consumers.
 *
 * A consumer that imports this platform gets one version for every surf-api module, so a transitive
 * path cannot pull a second version of a sibling module into the graph. The constraints are built
 * from the subproject names and this project's own coordinates rather than from the other projects'
 * state, because those projects are not configured yet while this one is.
 */

plugins {
    `java-platform`
    `maven-publish`
}

/**
 * Modules that `settings.gradle.kts` only includes outside CI, plus the KSP processor of the Gradle
 * plugin.
 */
val notPublished = setOf(
    "surf-api-bom",
    "surf-api-processor",
    "surf-api-generator",
    "surf-api-paper-plugin-test",
    "surf-api-minestom-example",
    "surf-api-minestom-example-plugin",
)

val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = findProperty("group") as String
version = findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""

dependencies {
    constraints {
        // The infra repositories are project trees whose parent projects only group modules.
        rootProject.subprojects
            .filter { it.subprojects.isEmpty() }
            .map { it.name }
            .filterNot { it in notPublished }
            .sorted()
            .forEach { api("$group:$it:$version") }
    }
}

publishing {
    repositories {
        maven("https://reposilite.slne.dev/releases/") {
            name = "slne-repository-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["javaPlatform"])
    }
}

description = "surf-api-bom"
