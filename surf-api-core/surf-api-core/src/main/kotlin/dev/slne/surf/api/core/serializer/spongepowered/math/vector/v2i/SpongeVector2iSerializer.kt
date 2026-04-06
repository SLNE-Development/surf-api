package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v2i

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector2i

typealias SerializableVector2i = @Serializable(with = SpongeVector2iSerializer::class) Vector2i

object SpongeVector2iSerializer : KSerializer<Vector2i> {
    private val arraySerializer = IntArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector2i", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector2i,
    ) {
        encoder.encodeSerializableValue(arraySerializer, intArrayOf(value.x(), value.y()))
    }

    override fun deserialize(decoder: Decoder): Vector2i {
        val (x, y) = decoder.decodeSerializableValue(arraySerializer)
        return Vector2i(x, y)
    }
}