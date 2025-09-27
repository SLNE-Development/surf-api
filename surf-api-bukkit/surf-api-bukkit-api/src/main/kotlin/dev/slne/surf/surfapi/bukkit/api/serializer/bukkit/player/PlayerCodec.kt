package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.player

import com.mojang.serialization.Codec
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object PlayerCodec {
    val CODEC: Codec<Player> = JavaUUIDCodec.CODEC.xmap(
        { uuid -> Bukkit.getPlayer(uuid) },
        { player -> player.uniqueId }
    )
}