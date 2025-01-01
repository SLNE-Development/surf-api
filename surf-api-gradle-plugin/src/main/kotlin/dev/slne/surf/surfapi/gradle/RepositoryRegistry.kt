package dev.slne.surf.surfapi.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

internal object RepositoryRegistry {

    private val repos = mapOf<String, String>(
        "maven-unsafe" to "https://repo.slne.dev/repository/maven-unsafe/",
        "maven-public" to "https://repo.slne.dev/repository/maven-public/",
        "maven-snapshots" to "https://repo.slne.dev/repository/maven-snapshots",
        "maven-proxy" to "https://repo.slne.dev/repository/maven-proxy",
        "maven-external-developers" to "https://repo.slne.dev/repository/maven-external-developers",
    )

    fun Project.applyRepositories() {
        repositories {
            mavenCentral()
            gradlePluginPortal()
        }

        for ((name, url) in repos) {
            repositories.maven(url) { this.name = name }
        }
    }
}