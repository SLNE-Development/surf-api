package dev.slne.surf.api.nats

import dev.slne.surf.nats.internal.NATS_CLIENT_NAME_PATTERN
import java.util.*

/**
 * Derives the NATS client name of a plugin from the plugin's name.
 */
internal object NatsClientNames {

    private val NAME_PATTERN = Regex(NATS_CLIENT_NAME_PATTERN)
    private val SEPARATORS = Regex("[^a-z0-9]+")

    /**
     * Returns the client name for [pluginName]: lowercase, every run of characters other than
     * letters and digits replaced by one hyphen, and no hyphen at either end.
     *
     * The name is visible on the wire: it is the queue group of `ANY_INSTANCE` listeners and the
     * origin of published events. Two plugins whose names normalise to the same value collide, and
     * the second one fails to create its client.
     *
     * @throws IllegalArgumentException if nothing usable remains, such as for a name without
     * letters or digits
     */
    fun of(pluginName: String): String {
        val name = pluginName.lowercase(Locale.ROOT).replace(SEPARATORS, "-").trim('-')
        require(NAME_PATTERN.matches(name)) {
            "Plugin name '$pluginName' yields no valid NATS client name (got '$name')"
        }

        return name
    }
}
