package dev.slne.surf.microservice.gradle

import org.gradle.api.Action
import org.gradle.api.tasks.Nested

/**
 * Configures the surf microservice plugin. The extension is registered as `surfMicroservice` in the project.
 *
 * Every setting has a default, so the block is optional. The settings sit in three nested blocks:
 * [database] for the database integration, [reliability] for the transactional services and [docker] for the
 * generated Dockerfile.
 *
 * ```kotlin
 * plugins {
 *     id("dev.slne.surf.api.gradle.standalone")
 *     id("dev.slne.surf.microservice")
 * }
 *
 * surfMicroservice {
 *     database {
 *         version = "2.4.2"
 *     }
 *     docker {
 *         overwrite = true
 *         baseImage = "eclipse-temurin:25-jre-noble"
 *     }
 * }
 * ```
 *
 * Values are read lazily and may be set at any point of the build script. A value set in the block takes precedence
 * over the Gradle property of the same setting, which in turn replaces the built-in default. The Gradle properties
 * are listed in [MicroservicePlugin].
 */
abstract class MicroserviceExtension {
    /**
     * Settings of the database integration.
     */
    @get:Nested
    abstract val database: MicroserviceDatabaseExtension

    /**
     * Settings of the reliability integration.
     */
    @get:Nested
    abstract val reliability: MicroserviceReliabilityExtension

    /**
     * Settings of the generated Dockerfile.
     */
    @get:Nested
    abstract val docker: MicroserviceDockerExtension

    /**
     * Configures [database] with [action], the function behind the `database { ... }` block.
     */
    fun database(action: Action<in MicroserviceDatabaseExtension>) {
        action.execute(database)
    }

    /**
     * Configures [reliability] with [action], the function behind the `reliability { ... }` block.
     */
    fun reliability(action: Action<in MicroserviceReliabilityExtension>) {
        action.execute(reliability)
    }

    /**
     * Configures [docker] with [action], the function behind the `docker { ... }` block.
     */
    fun docker(action: Action<in MicroserviceDockerExtension>) {
        action.execute(docker)
    }
}
