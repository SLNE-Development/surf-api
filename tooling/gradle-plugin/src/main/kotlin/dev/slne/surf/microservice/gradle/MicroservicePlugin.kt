package dev.slne.surf.microservice.gradle

import dev.slne.surf.microservice.gradle.dependencies.addMicroserviceDependencies
import dev.slne.surf.microservice.gradle.dependencies.addReliabilityDependencies
import dev.slne.surf.microservice.gradle.docker.applyDockerConventions
import dev.slne.surf.microservice.gradle.docker.applyJavaDockerConventions
import dev.slne.surf.microservice.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.shadow.configureMicroserviceShadow
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create

/**
 * Turns a project into a surf microservice. The plugin id is `dev.slne.surf.api.gradle.microservice`.
 *
 * ```kotlin
 * plugins {
 *     id("dev.slne.surf.api.gradle.standalone")
 *     id("com.gradleup.shadow")
 *     id("dev.slne.surf.api.gradle.microservice")
 * }
 * ```
 *
 * What the plugin does depends on the plugins applied next to it, in any order:
 * - `java-library`: adds `surf-api-microservice-api` to `api` and `surf-api-microservice-runtime` to
 *   `implementation`. Unless disabled, it also adds the database integration (`surf-api-microservice-database` and
 *   `surf-api-database-r2dbc`) and, with surf-nats, `surf-api-microservice-reliability` to `implementation`. The
 *   microservice and database modules use the major version of the plugin, for example `4.+`.
 * - `java`: derives the default Docker images from the Java toolchain, see [MicroserviceDockerExtension].
 * - `com.gradleup.shadow`: makes `shadowJar` the runnable microservice. Its `Main-Class` is the class set in
 *   [MicroserviceExtension.mainClass], by default the single class with a `public static void main(String[])` in the
 *   compiled main source set. The jar merges `META-INF/services` files, the `.imports` files in `META-INF/spring`
 *   and, per key, `META-INF/spring.factories`, so that the Spring Boot auto-configurations and context initializers of
 *   all modules are found. It also combines the log4j plugin caches, without which log4j finds no configuration. The
 *   plugin also registers the tasks below.
 *
 * These tasks exist only with the Shadow plugin:
 * - `detectMicroserviceMainClass`: resolves the main class for the `Main-Class` attribute. It fails if the project has
 *   no class with a `public static void main(String[])` or several, and [MicroserviceExtension.mainClass] is not set.
 * - `runMicroservice`: builds the jar and runs it with `java -jar` on the project's Java toolchain. The working
 *   directory is `run/` in the project directory, so configuration and data survive `clean`. Standard input is
 *   forwarded to the microservice console.
 * - `generateMicroserviceDockerfile`: writes the Dockerfile, see [MicroserviceDockerExtension].
 * - `prepareMicroserviceDockerArtifact`: copies the jar to `build/docker/app.jar`. The generated Dockerfile runs it
 *   during the image build.
 * - `generateMicroserviceCommandClientSource`: writes the `microservicectl` source to
 *   `build/generated/microservicectl/main.go`. `prepareMicroserviceDockerArtifact` depends on it.
 *
 * Settings live in the [MicroserviceExtension], `surfMicroservice { ... }`. A Gradle property, for example in
 * `gradle.properties`, sets the default of the matching setting:
 * - [DATABASE_ENABLED_PROPERTY]: [MicroserviceDatabaseExtension.enabled], `true` without it.
 * - [DATABASE_VERSION_PROPERTY]: [MicroserviceDatabaseExtension.version], [DEFAULT_DATABASE_VERSION] without it.
 * - [RELIABILITY_ENABLED_PROPERTY]: [MicroserviceReliabilityExtension.enabled], derived from the database integration
 *   and surf-nats without it.
 *
 * DSL values are read lazily.
 */
abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Shadow and surf-api types are only touched once their plugins are applied, so the plugin loads without them
        // on the build classpath. Nothing is configured in afterEvaluate.
        val extension = project.extensions.create<MicroserviceExtension>(EXTENSION_NAME)
        project.applyDatabaseConventions(extension.database)
        project.applyDockerConventions(extension.docker)

        with(project.pluginManager) {
            withPlugin("java-library") {
                project.addMicroserviceDependencies(extension.database)
                val explicitReliability = project.explicitReliability(extension.reliability)
                project.addReliabilityDependencies(extension.database, explicitReliability)
            }
            withPlugin("java") {
                project.applyJavaDockerConventions(extension.docker)
            }
            withPlugin("com.gradleup.shadow") {
                project.configureMicroserviceShadow(extension)
            }
        }
    }

    private fun Project.applyDatabaseConventions(database: MicroserviceDatabaseExtension) {
        database.enabled.convention(
            providers.gradleProperty(DATABASE_ENABLED_PROPERTY)
                .map { value -> parseBooleanProperty(DATABASE_ENABLED_PROPERTY, value) }
                .orElse(true)
        )
        database.version.convention(
            providers.gradleProperty(DATABASE_VERSION_PROPERTY).orElse(DEFAULT_DATABASE_VERSION)
        )
    }

    /**
     * Returns whether reliability was enabled or disabled explicitly, through the DSL or the Gradle property. The
     * provider is empty if neither sets it, so that the default can be derived and only an explicit choice is
     * validated.
     */
    private fun Project.explicitReliability(reliability: MicroserviceReliabilityExtension): Provider<Boolean> {
        return reliability.enabled.orElse(
            providers.gradleProperty(RELIABILITY_ENABLED_PROPERTY)
                .map { value -> parseBooleanProperty(RELIABILITY_ENABLED_PROPERTY, value) }
        )
    }

    companion object {
        /**
         * Name of the [MicroserviceExtension] in a project.
         */
        const val EXTENSION_NAME = "surfMicroservice"

        /**
         * Gradle property that sets the default of [MicroserviceDatabaseExtension.enabled]: `true` or `false`.
         */
        const val DATABASE_ENABLED_PROPERTY = "surf.microservice.database.enabled"

        /**
         * Gradle property that sets the default of [MicroserviceReliabilityExtension.enabled]: `true` or `false`.
         */
        const val RELIABILITY_ENABLED_PROPERTY = "surf.microservice.reliability.enabled"

        /**
         * Gradle property that sets the default of [MicroserviceDatabaseExtension.version].
         */
        const val DATABASE_VERSION_PROPERTY = "surf.database.version"

        /**
         * Version of `surf-api-database-r2dbc` used when neither the DSL nor [DATABASE_VERSION_PROPERTY] sets one.
         */
        const val DEFAULT_DATABASE_VERSION = Constants.SURF_MICROSERVICE_VERSION
    }
}

private fun parseBooleanProperty(name: String, value: String): Boolean =
    value.toBooleanStrictOrNull() ?: throw GradleException(
        "Gradle property '$name' must be 'true' or 'false', was '$value'."
    )
