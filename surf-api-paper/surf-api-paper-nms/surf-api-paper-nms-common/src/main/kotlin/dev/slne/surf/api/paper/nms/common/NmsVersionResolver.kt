package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.bukkit.Bukkit

object NmsVersionResolver {
    private val log = logger()

    /**
     * The NMS version of the currently running Minecraft server.
     *
     * Detected at runtime from `Bukkit.getMinecraftVersion()`.
     * Uses latest provided NMS version if no exact match is found.
     */
    val current: NmsVersion by lazy {
        val mcVersion = Bukkit.getMinecraftVersion()
        val matched = NmsVersion.entries.firstOrNull { mcVersion.startsWith(it.versionPrefix) }
        if (matched != null) {
            matched
        } else {
            val fallback = NmsVersion.entries.maxByOrNull { it.versionPrefix } ?: error("No NMS versions defined!")
            log.atWarning().log(
                "There is no matching nms version, using fallback NMS version: %s",
                fallback.name
            )
            fallback
        }
    }
}