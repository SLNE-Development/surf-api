package dev.slne.surf.nats.config

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsInstance
import java.nio.file.Path

/**
 * Locates the directory of the NATS `config.yml`.
 *
 * A host that embeds NATS installs the directory; otherwise the data path of the platform [NatsInstance] applies.
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
     * Returns the installed directory, or the data path of the [NatsInstance].
     */
    fun resolve(): Path = installed ?: NatsInstance.get().dataPath
}
