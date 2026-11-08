package dev.slne.surf.api.paper.serializer.bukkit.offlineplayer

import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

typealias SerializableOfflinePlayer = @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer

object OfflinePlayerSerializer : KSerializer<OfflinePlayer> {
    override val descriptor =
        SerialDescriptor("surfapi.bukkit.OfflinePlayer", JavaUUIDSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: OfflinePlayer,
    ) {
        encoder.encodeSerializableValue(JavaUUIDSerializer, value.uniqueId)
    }

    override fun deserialize(decoder: Decoder): OfflinePlayer {
        return Bukkit.getOfflinePlayer(decoder.decodeSerializableValue(JavaUUIDSerializer))
    }
}