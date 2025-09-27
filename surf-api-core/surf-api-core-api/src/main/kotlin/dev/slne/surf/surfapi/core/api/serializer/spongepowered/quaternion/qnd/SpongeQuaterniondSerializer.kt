package dev.slne.surf.surfapi.core.api.serializer.spongepowered.quaternion.qnd

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector4d

typealias SerializableQuaterniond = @Serializable(with = SpongeQuaterniondSerializer::class) Quaterniond

object SpongeQuaterniondSerializer : KSerializer<Quaterniond> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Quaterniond", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Quaterniond,
    ) {
        val vector4d = Vector4d(value.x(), value.y(), value.z(), value.w())
        encoder.encodeSerializableValue(arraySerializer, vector4d.toArray())
    }

    override fun deserialize(decoder: Decoder): Quaterniond {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Quaterniond(array[0], array[1], array[2], array[3])
    }
}