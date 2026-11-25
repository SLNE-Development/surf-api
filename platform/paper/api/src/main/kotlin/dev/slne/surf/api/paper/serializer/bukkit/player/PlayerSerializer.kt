package dev.slne.surf.api.paper.serializer.bukkit.player

import dev.slne.surf.api.serialization.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

typealias SerializablePlayer = @Serializable(with = PlayerSerializer::class) Player

/**
 * Serializes a [Player] as its UUID through the format's contextual [UUID] serializer, with
 * [JavaUUIDSerializer] as the fallback.
 *
 * [descriptor] always describes the fallback encoding. A format that registers its own
 * [UUID] serializer may write a different shape, which descriptor-driven checks such as
 * Json's map-key validation do not see.
 */
object PlayerSerializer : KSerializer<Player> {
    private val uuidSerializer = ContextualSerializer(UUID::class, JavaUUIDSerializer, emptyArray())

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "surfapi.bukkit.Player",
        JavaUUIDSerializer.descriptor,
    )

    override fun serialize(
        encoder: Encoder,
        value: Player,
    ) {
        encoder.encodeSerializableValue(uuidSerializer, value.uniqueId)
    }

    override fun deserialize(decoder: Decoder): Player {
        val uuid = decoder.decodeSerializableValue(uuidSerializer)

        return Bukkit.getPlayer(uuid)
            ?: throw IllegalStateException("Player with UUID $uuid is not online.")
    }
}