package dev.slne.surf.surfapi.gradle.util

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven

const val DEFAULT_USERNAME_ENV_PRIVATE = "SLNE_PRIVATE_REPO_USERNAME"
const val DEFAULT_PASSWORD_ENV_PRIVATE = "SLNE_PRIVATE_REPO_PASSWORD"

const val DEFAULT_USERNAME_SNAPSHOTS = "SLNE_SNAPSHOTS_REPO_USERNAME"
const val DEFAULT_PASSWORD_SNAPSHOTS = "SLNE_SNAPSHOTS_REPO_PASSWORD"

const val DEFAULT_USERNAME_RELEASES = "SLNE_RELEASES_REPO_USERNAME"
const val DEFAULT_PASSWORD_RELEASES = "SLNE_RELEASES_REPO_PASSWORD"

inline fun RepositoryHandler.slnePublic(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://repo.slne.dev/repository/maven-public/") {
        name = "maven-public"
        block()
    }

inline fun RepositoryHandler.slneProxy(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://repo.slne.dev/repository/maven-proxy/") {
        name = "maven-proxy"
        block()
    }

inline fun RepositoryHandler.slneSnapshots(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://repo.slne.dev/repository/maven-snapshots/") {
        name = "maven-snapshots"
        credentials {
            username = System.getenv(DEFAULT_USERNAME_SNAPSHOTS)
            password = System.getenv(DEFAULT_PASSWORD_SNAPSHOTS)
        }

        block()
    }


inline fun RepositoryHandler.slneReleases(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://repo.slne.dev/repository/maven-releases/") {
        name = "maven-releases"
        credentials {
            username = System.getenv(DEFAULT_USERNAME_RELEASES)
            password = System.getenv(DEFAULT_PASSWORD_RELEASES)
        }

        block()
    }


inline fun RepositoryHandler.slnePrivate(crossinline block: MavenArtifactRepository.() -> Unit = {}) =
    maven("https://repo.slne.dev/repository/maven-private/") {
        name = "maven-private"
        credentials {
            username = System.getenv(DEFAULT_USERNAME_ENV_PRIVATE)
            password = System.getenv(DEFAULT_PASSWORD_ENV_PRIVATE)
        }
        block()
    }
