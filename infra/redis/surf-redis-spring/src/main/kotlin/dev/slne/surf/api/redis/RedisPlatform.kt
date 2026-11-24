package dev.slne.surf.api.redis

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.nio.file.Path

/**
 * Platform facts the bundled Redis integration needs before any plugin loads.
 *
 * Each platform entry point calls [install] once, early enough that it precedes the first plugin
 * context: on Paper that is surf-api's bootstrapper, because plugins bootstrap before the root
 * context exists. Reading a value before [install] fails.
 */
@InternalSurfApi
object RedisPlatform {

    @Volatile
    private var installed: Installed? = null

    /**
     * surf-api's own data folder. The Redis configuration lives in its `redis` subfolder.
     */
    val dataDirectory: Path
        get() = installation().dataDirectory

    /**
     * Installs the platform facts, replacing earlier ones.
     *
     * @param dataDirectory surf-api's own data folder
     * @param pluginNameResolver names the plugin that owns a class; the simple class name when the
     * platform cannot tell
     */
    fun install(
        dataDirectory: Path,
        pluginNameResolver: (Class<*>) -> String = { it.simpleName },
    ) {
        installed = Installed(dataDirectory, pluginNameResolver)
    }

    /**
     * Returns the name of the plugin that owns [clazz].
     */
    fun pluginNameOf(clazz: Class<*>): String = installation().pluginNameResolver(clazz)

    private fun installation(): RedisPlatform.Installed {
        return checkNotNull(installed) {
            "RedisPlatform is not installed; the platform entry point must call RedisPlatform.install before any plugin loads"
        }
    }

    private class Installed(
        val dataDirectory: Path,
        val pluginNameResolver: (Class<*>) -> String,
    )
}
