package dev.slne.surf.api.gradle.util

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven

const val DEFAULT_USERNAME_ENV_PRIVATE = "SLNE_PRIVATE_REPO_USERNAME"
const val DEFAULT_PASSWORD_ENV_PRIVATE = "SLNE_PRIVATE_REPO_PASSWORD"

const val DEFAULT_USERNAME_RELEASES = "SLNE_RELEASES_REPO_USERNAME"
const val DEFAULT_PASSWORD_RELEASES = "SLNE_RELEASES_REPO_PASSWORD"

inline fun RepositoryHandler.slnePublic(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://reposilite.slne.dev/public/") {
        name = "slne-repository-public"
        block()
    }

inline fun RepositoryHandler.canvasMaven(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://maven.canvasmc.io/snapshots") {
        name = "Canvas"
        block()
    }

inline fun RepositoryHandler.slneReleases(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://reposilite.slne.dev/releases/") {
        name = "slne-repository-releases"
        credentials {
            username = System.getenv(DEFAULT_USERNAME_RELEASES)
            password = System.getenv(DEFAULT_PASSWORD_RELEASES)
        }

        block()
    }

inline fun RepositoryHandler.slnePrivate(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://reposilite.slne.dev/private/") {
        name = "slne-repository-private"
        credentials {
            username = System.getenv(DEFAULT_USERNAME_ENV_PRIVATE)
            password = System.getenv(DEFAULT_PASSWORD_ENV_PRIVATE)
        }
        block()
    }