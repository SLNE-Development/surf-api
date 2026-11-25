package dev.slne.surf.nats.config

import dev.slne.surf.nats.InternalNatsApi
import java.nio.file.Path

/**
 * Locates the directory of the NATS `config.yml`.
 *
 * The host that embeds NATS installs the directory through `EmbeddedNats.useConfigDirectory` before
 * the first connection.
 */
@InternalNatsApi
internal object NatsConfigLocation {

    @Volatile
    private var installed: Path? = null

    /**
     * Fixes the directory to [directory]. Installing the same directory again does nothing.
     */
    @Synchronized
    fun install(directory: Path) {
        val current = installed
        check(current == null || current == directory) {
            "The NATS configuration directory is already $current, cannot change it to $directory"
        }
        installed = directory
    }

    /**
     * Returns the installed directory.
     *
     * @throws IllegalStateException if no directory is installed yet.
     */
    fun resolve(): Path {
        return checkNotNull(installed) {
            "The NATS configuration directory is not set, call EmbeddedNats.useConfigDirectory first"
        }
    }
}
