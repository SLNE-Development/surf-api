package dev.slne.surf.microservice.gradle

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Configures the surf microservice plugin. The extension is registered as `surfMicroservice` in the
 * project.
 *
 * Every setting has a default, so the block is optional. [mainClass] names the entry point. The
 * other settings sit in three nested blocks: [database] for the database integration, [reliability]
 * for the transactional services and [docker] for the generated Dockerfile.
 *
 * ```kotlin
 * plugins {
 *     id("dev.slne.surf.api.gradle.standalone")
 *     id("dev.slne.surf.api.gradle.microservice")
 * }
 *
 * surfMicroservice {
 *     database {
 *         version = "4.0.0"
 *     }
 *     docker {
 *         overwrite = true
 *         baseImage = "eclipse-temurin:25-jre-noble"
 *     }
 * }
 * ```
 *
 * Values are read lazily and may be set at any point of the build script. A value set in the block
 * takes precedence over the Gradle property of the same setting, which in turn replaces the
 * built-in default. The Gradle properties are listed in [MicroservicePlugin].
 */
abstract class MicroserviceExtension {
    /**
     * Binary name of the main class that starts the microservice, for example `com.example.MainKt`.
     *
     * Optional. Without a value the plugin takes the single class with a
     * `public static void main(String[])` from the compiled classes of the main source set, and the
     * build fails if there are none or several. The value becomes the `Main-Class` of the shadow
     * jar. With the `application` plugin, `application.mainClass` serves as this value when it is
     * set and this one is not. Setting `shadowJar.mainClass` in the build script takes precedence
     * and skips the detection.
     *
     * A Kotlin microservice declares the class next to its `@SpringBootApplication` class:
     * ```kotlin
     * fun main(args: Array<String>) {
     *     runMicroservice<MyApplication>(args)
     * }
     * ```
     */
    abstract val mainClass: Property<String>

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
