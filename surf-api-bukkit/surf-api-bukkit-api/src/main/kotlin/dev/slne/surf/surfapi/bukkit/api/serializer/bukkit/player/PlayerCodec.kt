package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.player

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import org.bukkit.entity.Player

object PlayerCodec {
    val CODEC: Codec<Player> = JavaUUIDCodec.CODEC.comapFlatMap({ uuid ->
        val player = server.getPlayer(uuid)

        if (player != null) {
            DataResult.success(player)
        } else {
            DataResult.error { "Player with UUID $uuid is not online" }
        }
    }, Player::getUniqueId)
}