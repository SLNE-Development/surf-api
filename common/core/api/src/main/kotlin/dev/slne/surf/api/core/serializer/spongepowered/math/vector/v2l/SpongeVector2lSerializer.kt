package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v2l

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector2l

typealias SerializableVector2l = @Serializable(with = SpongeVector2lSerializer::class) Vector2l

object SpongeVector2lSerializer : KSerializer<Vector2l> {
    private val arraySerializer = LongArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector2l", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector2l,
    ) {
        encoder.encodeSerializableValue(arraySerializer, longArrayOf(value.x(), value.y()))
    }

    override fun deserialize(decoder: Decoder): Vector2l {
        val (x, y) = decoder.decodeSerializableValue(arraySerializer)
        return Vector2l(x, y)
    }
}