package dev.slne.surf.api.core.serializer.adventure.key

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key

typealias SerializableKey = @Serializable(with = AdventureKeySerializer::class) Key

object AdventureKeySerializer : KSerializer<Key> {
    override val descriptor = PrimitiveSerialDescriptor("surf.api.Key", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Key,
    ) {
        encoder.encodeString(value.asString())
    }

    override fun deserialize(decoder: Decoder): Key {
        return Key.key(decoder.decodeString())
    }
}