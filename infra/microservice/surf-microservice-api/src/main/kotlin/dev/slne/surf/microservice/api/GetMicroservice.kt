package dev.slne.surf.microservice.api

/**
 * Returns the running microservice as [M].
 *
 * @throws IllegalStateException if no microservice is running
 * @throws ClassCastException if the running microservice is not an [M]
 */
inline fun <reified M : Microservice> getMicroservice(): M = Microservice.instance as M
