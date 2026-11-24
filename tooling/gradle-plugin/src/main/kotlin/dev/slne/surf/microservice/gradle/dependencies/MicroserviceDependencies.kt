package dev.slne.surf.microservice.gradle.dependencies

import dev.slne.surf.microservice.gradle.MicroserviceDatabaseExtension
import dev.slne.surf.microservice.gradle.generated.Constants
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin.*
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies

private const val MICROSERVICE_GROUP = "dev.slne.surf.api"
private const val DATABASE_GROUP = "dev.slne.surf.api"
private const val DATABASE_ARTIFACT = "surf-api-database-r2dbc"

/**
 * Adds the microservice API (`api`), the runtime and, while [database] is enabled, the database
 * integration plus `surf-api-database-r2dbc` (all `implementation`). The runtime is on the compile
 * classpath because the microservice calls `runMicroservice` from its `main` function.
 *
 * Requires `java-library`. The database dependencies follow [database] until `implementation` is
 * first resolved.
 */
internal fun Project.addMicroserviceDependencies(database: MicroserviceDatabaseExtension) {
    dependencies {
        add(API_CONFIGURATION_NAME, microserviceModule("surf-api-microservice-api"))
        add(IMPLEMENTATION_CONFIGURATION_NAME, microserviceModule("surf-api-microservice-runtime"))
    }

    val databaseDependencies = databaseDependencies(database)
    configurations.named(IMPLEMENTATION_CONFIGURATION_NAME) {
        dependencies.addAllLater(databaseDependencies)
    }
}

private fun Project.databaseDependencies(
    database: MicroserviceDatabaseExtension,
): Provider<List<Dependency>> {
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
