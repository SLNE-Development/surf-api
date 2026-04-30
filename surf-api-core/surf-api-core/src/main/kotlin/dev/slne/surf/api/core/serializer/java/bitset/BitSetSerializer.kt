package dev.slne.surf.api.core.serializer.java.bitset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

typealias SerializableBitSet = @Serializable(with = BitSetSerializer::class) BitSet

object BitSetSerializer : KSerializer<BitSet> {
    private val longArraySerializer = LongArraySerializer()
    override val descriptor = SerialDescriptor("dev.slne.surf.api.BitSet", longArraySerializer.descriptor)

    override fun serialize(encoder: Encoder, value: BitSet) {
        longArraySerializer.serialize(encoder, value.toLongArray())
    }

    override fun deserialize(decoder: Decoder): BitSet {
        val longArray = longArraySerializer.deserialize(decoder)
        return BitSet.valueOf(longArray)
    }
}