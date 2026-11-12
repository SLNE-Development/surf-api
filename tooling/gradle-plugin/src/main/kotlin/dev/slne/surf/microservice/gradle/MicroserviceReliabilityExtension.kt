package dev.slne.surf.microservice.gradle

import org.gradle.api.provider.Property

/**
 * Controls the reliability integration that the plugin adds to the microservice: the transactional services of
 * `surf-api-microservice-reliability`, whose persistent handlers run once per message and publish their events after
 * the commit.
 *
 * The integration needs the database integration and surf-nats. It is enabled automatically when both are present, so
 * most projects never configure it. The setting is read when `implementation` or `testImplementation` is first
 * resolved, so it may be set after the plugin is applied.
 *
 * ```kotlin
 * surfMicroservice {
 *     reliability {
 *         // Opt out although the database and surf-nats are present
 *         enabled = false
 *     }
 * }
 * ```
 *
 * The same setting can come from `gradle.properties` or `-P` on the command line:
 *
 * ```properties
 * surf.microservice.reliability.enabled=false
 * ```
 */
abstract class MicroserviceReliabilityExtension {
    /**
     * Whether `surf-api-microservice-reliability` is added to `implementation` and its test fixtures to
     * `testImplementation`, in the version of the other microservice modules.
     *
     * The property has no value of its own until it is set, so reading it from a build script before that fails.
     * The effective choice is, in this order:
     * 1. the value set here,
     * 2. the Gradle property `surf.microservice.reliability.enabled` (`true` or `false`, any other value fails the
     *    build),
     * 3. enabled if the database integration is enabled and surf-nats is present, otherwise disabled. surf-nats is
     *    present if the `dev.slne.surf.api.gradle.nats` plugin is applied or `surf-api-nats-standalone` is declared in
     *    `api`, `implementation` or `runtimeOnly`.
     *
     * Enabling it explicitly, with the value or the property, while the database integration is disabled or surf-nats
     * is missing fails the build when the dependencies are resolved.
     */
    abstract val enabled: Property<Boolean>
}
