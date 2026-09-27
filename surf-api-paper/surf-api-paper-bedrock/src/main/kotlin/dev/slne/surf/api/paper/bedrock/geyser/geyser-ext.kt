package dev.slne.surf.api.paper.bedrock.geyser

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.connection.GeyserConnection

val geyserApi get() = GeyserApi.api()

val Player.geyserConnection get() = geyserApi.connectionByUuid(uniqueId)
fun Player.isBedrockPlayer() = geyserApi.isBedrockPlayer(uniqueId)
val Player.xuid get() = geyserApi.connectionByUuid(uniqueId)?.xuid()

val GeyserConnection.bukkitPlayer get() = Bukkit.getPlayer(this.javaUuid())
fun GeyserConnection.sendText(block: SurfComponentBuilder.() -> Unit) =
    bukkitPlayer?.sendMessage(SurfComponentBuilder().apply(block).build())

fun GeyserConnection.sendActionBar(component: Component) = bukkitPlayer?.sendActionBar(component)