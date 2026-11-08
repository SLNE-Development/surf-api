package dev.slne.surf.microservice.gradle

import org.gradle.api.provider.Property

/**
 * Controls the reliability integration that the plugin adds to the microservice: the transactional services of
 * `surf-microservice-reliability`, whose persistent handlers run once per message and publish their events after the
 * commit.
 *
 * Read when the dependencies are first resolved, so it may change after the plugin is applied.
 */
abstract class MicroserviceReliabilityExtension {
    /**
     * Whether `surf-microservice-reliability` is added to `implementation` and its test fixtures to
     * `testImplementation`, in the version of the other microservice modules.
     *
     * Defaults to the Gradle property `surf.microservice.reliability.enabled`, otherwise to whether the database
     * integration is enabled and surf-nats is present: the `dev.slne.surf.nats` plugin is applied or
     * `surf-nats-standalone` is declared in `api`, `implementation` or `runtimeOnly`. Enabling it explicitly, here or
     * through the property, while one of them is missing fails the build when the dependencies are resolved.
     */
    abstract val enabled: Property<Boolean>
}
