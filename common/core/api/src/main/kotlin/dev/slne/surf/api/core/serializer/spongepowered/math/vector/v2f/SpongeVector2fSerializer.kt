package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v2f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector2f

typealias SerializableVector2f = @Serializable(with = SpongeVector2fSerializer::class) Vector2f

object SpongeVector2fSerializer : KSerializer<Vector2f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector2f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector2f,
    ) {
        encoder.encodeSerializableValue(arraySerializer, floatArrayOf(value.x(), value.y()))
    }

    override fun deserialize(decoder: Decoder): Vector2f {
        val (x, y) = decoder.decodeSerializableValue(arraySerializer)
        return Vector2f(x, y)
    }
}