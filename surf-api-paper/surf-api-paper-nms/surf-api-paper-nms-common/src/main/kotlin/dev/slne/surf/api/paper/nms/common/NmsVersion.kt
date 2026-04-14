package dev.slne.surf.api.paper.nms.common

import org.bukkit.Bukkit

/**
 * Represents the supported Minecraft NMS versions.
 *
 * @property versionPrefix The version prefix used to match `Bukkit.getMinecraftVersion()`.
 */
enum class NmsVersion(val versionPrefix: String) {
    V1_21_11("1.21"),
    V26_1("26.");

    companion object {
        /**
         * The NMS version of the currently running Minecraft server.
         *
         * Detected at runtime from `Bukkit.getMinecraftVersion()`.
         * Uses latest provided NMS version if no exact match is found.
         */
        val current: NmsVersion by lazy {
            val mcVersion = Bukkit.getMinecraftVersion()
            entries.firstOrNull { mcVersion.startsWith(it.versionPrefix) }
                ?: entries.maxByOrNull { it.versionPrefix } ?: error("No NMS versions defined!")
        }
    }
}
