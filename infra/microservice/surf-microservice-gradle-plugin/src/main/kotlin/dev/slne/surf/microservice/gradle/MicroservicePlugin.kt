package dev.slne.surf.microservice.gradle

import dev.slne.surf.microservice.gradle.dependencies.addMicroserviceDependencies
import dev.slne.surf.microservice.gradle.dependencies.addMicroserviceProcessor
import dev.slne.surf.microservice.gradle.dependencies.addReliabilityDependencies
import dev.slne.surf.microservice.gradle.dependencies.rejectSurfApiDatabase
import dev.slne.surf.microservice.gradle.docker.applyDockerConventions
import dev.slne.surf.microservice.gradle.docker.applyJavaDockerConventions
import dev.slne.surf.microservice.gradle.shadow.configureMicroserviceShadow
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create

/**
 * Turns a project into a surf microservice.
 *
 * Adds the microservice API, runtime, KSP processor and, unless disabled, the database integration and, with
 * surf-nats, the reliability integration. With Shadow applied it also configures the runnable jar and registers
 * `runMicroservice` and the Docker tasks.
 *
 * Shadow and surf-api types are only touched once their plugins are applied, so the plugin loads without them
 * on the build classpath. Nothing is configured in `afterEvaluate`; DSL values are read lazily.
 */
abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<MicroserviceExtension>(EXTENSION_NAME)
        project.applyDatabaseConventions(extension.database)
        project.applyDockerConventions(extension.docker)

        with(project.pluginManager) {
            withPlugin("java-library") {
                project.addMicroserviceDependencies(extension.database)
                val explicitReliability = project.explicitReliability(extension.reliability)
                project.addReliabilityDependencies(extension.database, explicitReliability)
                project.rejectSurfApiDatabase()
            }
            withPlugin("com.google.devtools.ksp") {
                project.addMicroserviceProcessor()
            }
            withPlugin("java") {
                project.applyJavaDockerConventions(extension.docker)
            }
            withPlugin("com.gradleup.shadow") {
                project.configureMicroserviceShadow(extension.docker)
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
     * Returns whether reliability was enabled or disabled explicitly, through the DSL or the Gradle property; empty if
     * neither sets it, so that the default can be derived and only an explicit choice is validated.
     */
    private fun Project.explicitReliability(reliability: MicroserviceReliabilityExtension): Provider<Boolean> {
        return reliability.enabled.orElse(
            providers.gradleProperty(RELIABILITY_ENABLED_PROPERTY)
                .map { value -> parseBooleanProperty(RELIABILITY_ENABLED_PROPERTY, value) }
        )
    }

    companion object {
        const val EXTENSION_NAME = "surfMicroservice"
        const val DATABASE_ENABLED_PROPERTY = "surf.microservice.database.enabled"
        const val RELIABILITY_ENABLED_PROPERTY = "surf.microservice.reliability.enabled"
        const val DATABASE_VERSION_PROPERTY = "surf.database.version"
        const val DEFAULT_DATABASE_VERSION = "2.+"
    }
}

private fun parseBooleanProperty(name: String, value: String): Boolean =
    value.toBooleanStrictOrNull() ?: throw GradleException(
        "Gradle property '$name' must be 'true' or 'false', was '$value'."
    )
