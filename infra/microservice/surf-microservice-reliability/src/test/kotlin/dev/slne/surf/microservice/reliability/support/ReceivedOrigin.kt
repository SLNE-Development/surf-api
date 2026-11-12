package dev.slne.surf.microservice.reliability.support

/**
 * Origin of one received event as its listener saw it.
 */
data class ReceivedOrigin(val message: String, val origin: String?, val isFromThisClient: Boolean)
