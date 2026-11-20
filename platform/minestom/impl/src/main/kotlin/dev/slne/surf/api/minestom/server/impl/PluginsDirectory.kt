package dev.slne.surf.api.minestom.server.impl

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.nio.file.Path

/**
 * The plugins directory of a Minestom server as the [PROPERTY] system property names it.
 *
 * The plugins feature loads plugins from it by default, and the server runtime keeps surf-api's
 * own data folder in it, so both must resolve it the same way. The server bootstrap
 * (`ServerPlugins` in the bootstrap module) runs before this module is on the class path and
 * declares [PROPERTY] a second time; the two values must match.
 */
@InternalSurfApi
object PluginsDirectory {

    /**
     * The system property that moves the plugins directory.
     */
    const val PROPERTY: String = "surf.minestom.plugins"

    /**
     * The plugins directory when [PROPERTY] is not set, relative to the working directory.
     */
    const val DEFAULT: String = "plugins"

    /**
     * Resolves the directory from [PROPERTY], or [DEFAULT] when it is not set.
     */
    fun fromSystemProperty(): Path = Path.of(System.getProperty(PROPERTY, DEFAULT))
}
