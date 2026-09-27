package dev.slne.surf.api.paper.bedrock.geyser

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.bedrock.BedrockSupport
import dev.slne.surf.api.paper.bedrock.ifBedrockAvailable
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.connection.GeyserConnection

/**
 * The Geyser API instance.
 *
 * @throws IllegalStateException if Geyser or Floodgate is not installed.
 */
val geyserApi: GeyserApi
    get() {
        BedrockSupport.requireAvailable()
        return GeyserApi.api()
    }

/**
 * The Geyser connection of this player, or `null` if this player is not a Bedrock player
 * or Geyser/Floodgate is not installed.
 */
val Player.geyserConnection: GeyserConnection?
    get() = ifBedrockAvailable(null) { geyserApi.connectionByUuid(uniqueId) }

/**
 * Returns `true` if this player is a Bedrock player.
 * Returns `false` if Geyser/Floodgate is not installed.
 */
fun Player.isBedrockPlayer() = ifBedrockAvailable(false) { geyserApi.isBedrockPlayer(uniqueId) }

/**
 * The Xbox user id of this player, or `null` if this player is not a Bedrock player
 * or Geyser/Floodgate is not installed.
 */
val Player.xuid: String? get() = geyserConnection?.xuid()

val GeyserConnection.bukkitPlayer get() = Bukkit.getPlayer(this.javaUuid())
fun GeyserConnection.sendText(block: SurfComponentBuilder.() -> Unit) =
    bukkitPlayer?.sendMessage(SurfComponentBuilder().apply(block).build())

fun GeyserConnection.sendActionBar(component: Component) = bukkitPlayer?.sendActionBar(component)
