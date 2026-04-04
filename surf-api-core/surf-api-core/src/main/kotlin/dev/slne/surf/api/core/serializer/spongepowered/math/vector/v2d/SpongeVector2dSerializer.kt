package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v2d

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector2d

typealias SerializableVector2d = @Serializable(with = SpongeVector2dSerializer::class) Vector2d

object SpongeVector2dSerializer : KSerializer<Vector2d> {
    private val doubleArraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector2d", doubleArraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector2d,
    ) {
        encoder.encodeSerializableValue(doubleArraySerializer, doubleArrayOf(value.x(), value.y()))
    }

    override fun deserialize(decoder: Decoder): Vector2d {
        val (x, y) = decoder.decodeSerializableValue(doubleArraySerializer)
        return Vector2d(x, y)
    }
}