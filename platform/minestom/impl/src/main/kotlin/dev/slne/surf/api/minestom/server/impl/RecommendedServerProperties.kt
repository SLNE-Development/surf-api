package dev.slne.surf.api.minestom.server.impl

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.property.ServerProperties
import net.minestom.server.property.ServerProperty

/**
 * The settings applied by `SurfMinestomServerBuilder.recommendedServerProperties`.
 *
 * Values chosen explicitly are kept: a property set with `-D`, or a compression threshold that
 * differs from Minestom's default. Minestom properties are writable at runtime only when
 * `<name>.mutable` is set before they are first read, which the bootstrap of the server jar does;
 * otherwise a warning is logged and the value stays.
 */
internal object RecommendedServerProperties {
    private val LOGGER = ComponentLogger.logger()

    /**
     * Keep-alive interval in milliseconds. Velocity drops a backend that stays silent for its
     * `login-timeout` (6 seconds by default) before the player reaches the play phase, while the
     * configuration phase may hold a player indefinitely.
     */
    private const val KEEP_ALIVE_DELAY_MILLIS = 2_000L

    /**
     * The connection to the proxy is local, so compression only costs CPU.
     */
    private const val COMPRESSION_THRESHOLD = 0

    /**
     * Minestom's default compression threshold; a different value means the server chose one.
     */
    private const val MINESTOM_COMPRESSION_THRESHOLD = 256

    fun apply() {
        apply(ServerProperties.KEEP_ALIVE_DELAY, KEEP_ALIVE_DELAY_MILLIS)
        applyCompressionThreshold()
    }

    private fun applyCompressionThreshold() {
        if (MinecraftServer.process()?.isAlive == true) {
            LOGGER.warn("The server already started, packet compression stays as it is")
            return
        }

        val current = MinecraftServer.getCompressionThreshold()
        if (current != MINESTOM_COMPRESSION_THRESHOLD) {
            LOGGER.info("The compression threshold is set to {}, keeping it", current)
            return
        }

        MinecraftServer.setCompressionThreshold(COMPRESSION_THRESHOLD)
        LOGGER.info("Disabled packet compression")
    }

    private fun <T : Any> apply(property: ServerProperty<T>, value: T) {
        val name = property.name()
        when {
            System.getProperty(name) != null ->
                LOGGER.info("{} is set to {} with -D{}, keeping it", name, property.get(), name)

            !property.writable() ->
                LOGGER.warn("{} cannot be changed at runtime, set it with -D{}={}", name, name, value)

            else -> {
                property.set(value)
                LOGGER.info("Set {} to {}", name, value)
            }
        }
    }
}
