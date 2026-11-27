plugins {
    `java-platform`
    `maven-publish`
}

/**
 * Modules that `settings.gradle.kts` only includes outside CI.
 */
val notPublished = setOf(
    "surf-api-bom",
    "surf-api-registry-key-generator",
    "surf-api-paper-plugin-test",
    "surf-api-minestom-example",
    "surf-api-minestom-example-plugin",
    "surf-api-paper-shared-bean-provider",
    "surf-api-paper-shared-bean-consumer",
    "surf-api-paper-shared-bean-test",
    "surf-api-paper-server-harness",
    "surf-api-paper-scenario-probe",
    "surf-api-paper-scenario-owner",
    "surf-api-paper-scenario-test",
    "surf-api-velocity-shared-bean-provider",
    "surf-api-velocity-shared-bean-consumer",
    "surf-api-velocity-shared-bean-test",
)

val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = findProperty("group") as String
version = findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""

dependencies {
    constraints {
        // The infra repositories are project trees whose parent projects only group modules.
        val publishedModules = rootProject.subprojects
            .filter { it.subprojects.isEmpty() }
            .map { it.name }
            .filterNot { it in notPublished }
            .sorted()
        for (module in publishedModules) {
            api("$group:$module:$version")
        }
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
