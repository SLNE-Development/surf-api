package dev.slne.surf.microservice.reliability

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.reliability.integration.ReliabilityHolder

/**
 * Gets the transactional event publisher of the running microservice.
 *
 * Available from `onBootstrap` until the end of `onDisable`. [TransactionalEvents.publish] works only inside handlers
 * of services registered with [registerTransactionalService].
 *
 * @throws IllegalStateException if the reliability integration is not running
 */
@Suppress("UnusedReceiverParameter")
public val Microservice.events: TransactionalEvents
    get() = ReliabilityHolder.runtime.events
