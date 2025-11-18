package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.player

import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.entity.Player

typealias SerializablePlayer = @Serializable(with = PlayerSerializer::class) Player

object PlayerSerializer : KSerializer<Player> {
    override val descriptor =
        SerialDescriptor("surfapi.bukkit.Player", JavaUUIDSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Player,
    ) {
        encoder.encodeSerializableValue(JavaUUIDSerializer, value.uniqueId)
    }

    override fun deserialize(decoder: Decoder): Player {
        val uuid = decoder.decodeSerializableValue(JavaUUIDSerializer)

        return Bukkit.getPlayer(uuid)
            ?: throw IllegalStateException("Player with UUID $uuid is not online, cannot deserialize to Player. Use OfflinePlayer instead.")
    }
}