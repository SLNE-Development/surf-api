package dev.slne.surf.surfapi.gradle.util

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.slnePublic() = maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
fun RepositoryHandler.slnePrivate() = maven("https://repo.slne.dev/repository/maven-private/") { name = "maven-private" }
