package dev.slne.surf.surfapi.gradle.util

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.slneUnsafe() = maven("https://repo.slne.dev/repository/maven-unsafe/") { name = "maven-unsafe" }
fun RepositoryHandler.slnePublic() = maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
fun RepositoryHandler.slneSnapshots() = maven("https://repo.slne.dev/repository/maven-snapshots/") { name = "maven-snapshots" }
fun RepositoryHandler.slneProxy() = maven("https://repo.slne.dev/repository/maven-proxy/") { name = "maven-proxy" }
fun RepositoryHandler.slneExternalDevelopers() = maven("https://repo.slne.dev/repository/maven-external-developers/") { name = "maven-external-developers" }