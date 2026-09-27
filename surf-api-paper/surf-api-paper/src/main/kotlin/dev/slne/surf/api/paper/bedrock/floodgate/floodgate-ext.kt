package dev.slne.surf.api.paper.bedrock.floodgate

import dev.slne.surf.api.paper.bedrock.BedrockSupport
import dev.slne.surf.api.paper.bedrock.ifBedrockAvailable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.floodgate.api.player.FloodgatePlayer

/**
 * The Floodgate API instance.
 *
 * @throws IllegalStateException if Geyser or Floodgate is not installed.
 */
val floodgateApi: FloodgateApi
    get() {
        BedrockSupport.requireAvailable()
        return FloodgateApi.getInstance()
    }

/**
 * The Floodgate player of this player, or `null` if this player is not a Bedrock player
 * or Geyser/Floodgate is not installed.
 */
val Player.floodgatePlayer: FloodgatePlayer?
    get() = ifBedrockAvailable(null) { floodgateApi.getPlayer(uniqueId) }

val FloodgatePlayer.bukkitPlayer get() = Bukkit.getPlayer(this.javaUniqueId)

/**
 * Returns `true` if this player is a Floodgate player.
 * Returns `false` if Geyser/Floodgate is not installed.
 */
fun FloodgatePlayer.isFloodgatePlayer() =
    ifBedrockAvailable(false) { floodgateApi.isFloodgatePlayer(this.javaUniqueId) }
