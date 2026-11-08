package dev.slne.surf.microservice.gradle.dependencies

import dev.slne.surf.microservice.gradle.MicroserviceDatabaseExtension
import dev.slne.surf.microservice.gradle.MicroservicePlugin
import dev.slne.surf.microservice.gradle.generated.Constants
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
import org.gradle.api.provider.Provider

private const val RELIABILITY_MODULE = "dev.slne.surf.microservice:surf-microservice-reliability"
private const val NATS_PLUGIN = "dev.slne.surf.nats"
private const val NATS_GROUP = "dev.slne.surf.nats"
private const val NATS_STANDALONE = "surf-nats-standalone"
private val NATS_CONFIGURATIONS = listOf(
    API_CONFIGURATION_NAME,
    IMPLEMENTATION_CONFIGURATION_NAME,
    RUNTIME_ONLY_CONFIGURATION_NAME,
)

/**
 * Adds `surf-microservice-reliability` to `implementation` and its test fixtures to `testImplementation` while
 * reliability is enabled.
 *
 * [explicitChoice] holds the value set through the DSL or the Gradle property, if any. Without it, reliability is
 * enabled while [database] is enabled and surf-nats is present. The decision is made in `withDependencies`, when a
 * configuration built from `implementation` or `testImplementation` resolves, because surf-nats may be declared in
 * `implementation` itself.
 *
 * Requires `java-library`. Resolution fails with a [GradleException] if reliability is enabled explicitly while the
 * database is disabled or surf-nats is missing.
 */
internal fun Project.addReliabilityDependencies(
    database: MicroserviceDatabaseExtension,
    explicitChoice: Provider<Boolean>,
) {
    val version = Constants.SURF_MICROSERVICE_VERSION
    val reliability = dependencies.create("$RELIABILITY_MODULE:$version")
    val testFixtures = dependencies.testFixtures("$RELIABILITY_MODULE:$version")
    val isEnabled = { reliabilityEnabled(database, explicitChoice) }

    configurations.named(IMPLEMENTATION_CONFIGURATION_NAME) {
        withDependencies { if (isEnabled()) add(reliability) }
    }
    configurations.named(TEST_IMPLEMENTATION_CONFIGURATION_NAME) {
        withDependencies { if (isEnabled()) add(testFixtures) }
    }
}

private fun Project.reliabilityEnabled(
    database: MicroserviceDatabaseExtension,
    explicitChoice: Provider<Boolean>,
): Boolean {
    val databaseEnabled = database.enabled.get()
    val natsPresent = hasSurfNats()
    val choice = explicitChoice.orNull ?: return databaseEnabled && natsPresent

    if (choice && !databaseEnabled) {
        throw reliabilityRequirementMissing(
            "the database integration is disabled. Enable it with surfMicroservice { database { enabled = true } }"
        )
    }
    if (choice && !natsPresent) {
        throw reliabilityRequirementMissing(
            "surf-nats is missing. Apply the '$NATS_PLUGIN' plugin or add $NATS_GROUP:$NATS_STANDALONE"
        )
    }
    return choice
}

private fun Project.reliabilityRequirementMissing(reason: String) = GradleException(
    "Project '$path' enables surf-microservice-reliability (surfMicroservice { reliability { enabled = true } } or " +
            "Gradle property '${MicroservicePlugin.RELIABILITY_ENABLED_PROPERTY}'), but $reason, or disable " +
            "reliability."
)

private fun Project.hasSurfNats(): Boolean =
    pluginManager.hasPlugin(NATS_PLUGIN) || NATS_CONFIGURATIONS.any { name ->
        configurations.findByName(name)?.dependencies.orEmpty().any { it.isSurfNatsStandalone() }
    }

private fun Dependency.isSurfNatsStandalone(): Boolean = group == NATS_GROUP && name == NATS_STANDALONE
