package dev.slne.surf.api.paper.bedrock.floodgate

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.floodgate.api.player.FloodgatePlayer

val floodgateApi: FloodgateApi get() = FloodgateApi.getInstance()

val Player.floodgatePlayer: FloodgatePlayer? get() = floodgateApi.getPlayer(uniqueId)
val FloodgatePlayer.bukkitPlayer get() = Bukkit.getPlayer(this.javaUniqueId)
fun FloodgatePlayer.isFloodgatePlayer() = floodgateApi.isFloodgatePlayer(this.javaUniqueId)