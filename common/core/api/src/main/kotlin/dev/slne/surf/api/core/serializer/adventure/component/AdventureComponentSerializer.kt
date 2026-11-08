package dev.slne.surf.api.core.serializer.adventure.component

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

typealias SerializableComponent = @Serializable(with = AdventureComponentSerializer::class) Component

object AdventureComponentSerializer : KSerializer<Component> {
    override val descriptor =
        PrimitiveSerialDescriptor("surf.api.AdventureComponent", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Component,
    ) {
        encoder.encodeString(GsonComponentSerializer.gson().serialize(value.compact()))
    }

    override fun deserialize(decoder: Decoder): Component {
        return GsonComponentSerializer.gson().deserialize(decoder.decodeString())
    }
}