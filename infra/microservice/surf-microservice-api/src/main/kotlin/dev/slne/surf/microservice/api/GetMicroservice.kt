package dev.slne.surf.microservice.api

/**
 * Returns the running microservice as [M].
 *
 * ```kotlin
 * val punish = getMicroservice<PunishMicroservice>()
 * ```
 *
 * @throws IllegalStateException if the runtime has not created the microservice yet
 * @throws ClassCastException if the running microservice is not an [M]
 */
public inline fun <reified M : Microservice> getMicroservice(): M = Microservice.instance as M
