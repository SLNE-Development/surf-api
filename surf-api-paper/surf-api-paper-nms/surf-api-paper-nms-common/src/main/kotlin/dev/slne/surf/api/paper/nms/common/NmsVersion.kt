package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.core.util.logger
import org.bukkit.Bukkit

/**
 * Represents the supported Minecraft NMS versions.
 *
 * @property versionPrefix The version prefix used to match `Bukkit.getMinecraftVersion()`.
 */
enum class NmsVersion(val versionPrefix: String) {
    V1_21_11("1.21.11"),
    V26_1("26.1");

    companion object {
        private val log = logger()

        /**
         * The NMS version of the currently running Minecraft server.
         *
         * Detected at runtime from `Bukkit.getMinecraftVersion()`.
         * Uses latest provided NMS version if no exact match is found.
         */
        val current: NmsVersion by lazy {
            val mcVersion = Bukkit.getMinecraftVersion()
            log.atInfo().log("Detected Minecraft version from Bukkit: '%s'", mcVersion)
            log.atInfo().log("Available NMS versions: %s", entries.joinToString(", ") { "${it.name}(${it.versionPrefix})" })

            val matched = entries.firstOrNull { mcVersion.startsWith(it.versionPrefix) }
            if (matched != null) {
                log.atInfo().log("Selected NMS version: %s", matched.name)
                matched
            } else {
                log.atWarning().log("No exact NMS version match found, using fallback (maxByOrNull)")
                val fallback = entries.maxByOrNull { it.versionPrefix } ?: error("No NMS versions defined!")
                log.atWarning().log("Fallback NMS version selected: %s", fallback.name)
                fallback
            }
        }
    }
}
