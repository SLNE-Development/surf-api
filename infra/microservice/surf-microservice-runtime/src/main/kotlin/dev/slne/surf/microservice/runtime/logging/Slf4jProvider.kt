package dev.slne.surf.microservice.runtime.logging

/**
 * Pins SLF4J to the log4j provider.
 *
 * The runtime class path holds a second provider, slf4j-simple, which spark-standalone-agent bundles unrelocated.
 * Without the pin SLF4J warns about multiple providers and binds the first one its service lookup returns. SLF4J
 * 2.0.9 and later read [PROPERTY] once, when the first logger is created, and then skip the lookup, so [selectLog4j]
 * must run before anything logs.
 */
internal object Slf4jProvider {
    const val PROPERTY = "slf4j.provider"
    const val LOG4J = "org.apache.logging.slf4j.SLF4JServiceProvider"

    /**
     * Sets [PROPERTY] to [LOG4J] unless it is already set, for example with `-Dslf4j.provider=...`.
     */
    fun selectLog4j() {
        System.getProperties().putIfAbsent(PROPERTY, LOG4J)
    }
}
