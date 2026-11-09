package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.common.util.logger
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

        NmsVersion.matching(mcVersion) ?: NmsVersion.NEWEST.also { fallback ->
            log.atWarning().log(
                "No NMS bindings for Minecraft %s, falling back to the newest surf-api has: %s. " +
                        "Anything that reads a field or calls a method that moved since then will " +
                        "fail at the point it is used, not here.",
                mcVersion,
                fallback.versionPrefix,
            )
        }
    }
}