package dev.slne.surf.microservice.gradle.dependencies

import dev.slne.surf.microservice.gradle.MicroserviceDatabaseExtension
import dev.slne.surf.microservice.gradle.generated.Constants
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin.*
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies

private const val MICROSERVICE_GROUP = "dev.slne.surf.api"
private const val DATABASE_GROUP = "dev.slne.surf.api"
private const val DATABASE_ARTIFACT = "surf-api-database-r2dbc"
private const val KSP_CONFIGURATION = "ksp"

/**
 * Adds the microservice API (`api`), runtime (`runtimeOnly`) and, while [database] is enabled, the database
 * integration plus `surf-api-database-r2dbc` (`implementation`).
 *
 * Requires `java-library`. The database dependencies follow [database] until `implementation` is first resolved.
 */
internal fun Project.addMicroserviceDependencies(database: MicroserviceDatabaseExtension) {
    dependencies {
        add(API_CONFIGURATION_NAME, microserviceModule("surf-api-microservice-api"))
        add(RUNTIME_ONLY_CONFIGURATION_NAME, microserviceModule("surf-api-microservice-runtime"))
    }

    val databaseDependencies = databaseDependencies(database)
    configurations.named(IMPLEMENTATION_CONFIGURATION_NAME) {
        dependencies.addAllLater(databaseDependencies)
    }
}

/**
 * Adds the microservice KSP processor to `ksp`, whenever KSP creates that configuration.
 */
internal fun Project.addMicroserviceProcessor() {
    val processor = microserviceModule("surf-api-microservice-processor")
    configurations.named { name -> name == KSP_CONFIGURATION }.configureEach {
        dependencies.add(processor)
    }
}

/**
 * Fails the resolution of every classpath built from `api` once surf-api's `withSurfDatabaseR2dbc` added
 * `surf-api-database-r2dbc` there.
 *
 * surf-api adds that dependency in `afterEvaluate` with version `+`, which would bypass the major version that
 * [MicroserviceDatabaseExtension.version] pins, and relocates the package away from the database integration.
 */
internal fun Project.rejectSurfApiDatabase() {
    val projectPath = path
    configurations.named(API_CONFIGURATION_NAME) {
        withDependencies {
            if (any { dependency -> dependency.isSurfDatabase() }) {
                throw GradleException(
                    "Project '$projectPath' declares $DATABASE_GROUP:$DATABASE_ARTIFACT in 'api', which surf-api's " +
                            "withSurfDatabaseR2dbc(...) adds. Remove withSurfDatabaseR2dbc: surfMicroservice manages " +
                            "the database dependency. Set its version with surfMicroservice { database { version = " +
                            "\"...\" } } or disable it with database { enabled = false }."
                )
            }
        }
    }
}

private fun Project.databaseDependencies(database: MicroserviceDatabaseExtension): Provider<List<Dependency>> {
    val factory = dependencyFactory
    val integration = microserviceModule("surf-api-microservice-database")
    val none = providers.provider { emptyList<Dependency>() }

    return database.enabled.flatMap { enabled ->
        if (enabled) {
            database.version.map { version ->
                listOf(integration, factory.create(DATABASE_GROUP, DATABASE_ARTIFACT, version))
            }
        } else {
            none
        }
    }
}

private fun Project.microserviceModule(artifact: String): Dependency {
    return dependencyFactory.create(
        MICROSERVICE_GROUP,
        artifact,
        Constants.SURF_MICROSERVICE_VERSION,
    )
}

private fun Dependency.isSurfDatabase(): Boolean {
    return group == DATABASE_GROUP && name == DATABASE_ARTIFACT
}
