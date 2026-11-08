package dev.slne.surf.api.core.serializer.spongepowered.math.quaternion.qnf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.imaginary.Quaternionf
import org.spongepowered.math.vector.Vector4f

typealias SerializableQuaternionf = @Serializable(with = SpongeQuaternionfSerializer::class) Quaternionf

object SpongeQuaternionfSerializer : KSerializer<Quaternionf> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Quaternionf", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Quaternionf,
    ) {
        val vector4f = Vector4f(value.x(), value.y(), value.z(), value.w())
        encoder.encodeSerializableValue(arraySerializer, vector4f.toArray())
    }

    override fun deserialize(decoder: Decoder): Quaternionf {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Quaternionf(array[0], array[1], array[2], array[3])
    }
}