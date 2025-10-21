package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.offlineplayer

import com.mojang.serialization.Codec
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import org.bukkit.OfflinePlayer

object OfflinePlayerCodec {
    val CODEC: Codec<OfflinePlayer> = JavaUUIDCodec.CODEC
        .xmap(server::getOfflinePlayer, OfflinePlayer::getUniqueId)
}