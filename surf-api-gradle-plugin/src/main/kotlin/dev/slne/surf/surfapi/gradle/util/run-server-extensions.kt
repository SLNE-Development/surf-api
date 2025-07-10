package dev.slne.surf.surfapi.gradle.util

import dev.slne.surf.surfapi.gradle.generated.Constants
import xyz.jpenilla.runpaper.task.RunServer

/**
 * Adds the Surf API Bukkit server to the RunServer task.
 *
 * @receiver RunServer The RunServer task to add the Surf API Bukkit server to.
 */
fun RunServer.withSurfApiBukkit() {
    minecraftVersion(Constants.MINECRAFT_VERSION)
    
    downloadPlugins {
        modrinth("commandapi", Constants.COMMAND_API_VERSION)
        modrinth("luckperms", Constants.LUCKPERMS_VERSION)
        modrinth("packetevents", "${Constants.PACKETEVENTS_VERSION}+spigot")

        hangar("PlaceholderAPI", Constants.PLACEHOLDER_API_VERSION)

        github(
            "SLNE-Development",
            "surf-api",
            "v${Constants.SURF_API_FULL_VERSION}",
            "surf-api-bukkit-server-${Constants.SURF_API_FULL_VERSION}-all.jar"
        )
    }
}