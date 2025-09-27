package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.offlineplayer

import com.mojang.serialization.Codec
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

object OfflinePlayerCodec {
    val CODEC: Codec<OfflinePlayer> = JavaUUIDCodec.CODEC.xmap(
        { uuid -> Bukkit.getOfflinePlayer(uuid) },
        { player -> player.uniqueId }
    )
}