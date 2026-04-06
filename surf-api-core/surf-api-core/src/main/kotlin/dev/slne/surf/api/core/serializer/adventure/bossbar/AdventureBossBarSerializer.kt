@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.adventure.bossbar

import dev.slne.surf.api.core.serializer.adventure.component.AdventureComponentSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.serializer
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component

typealias SerializableBossBar = @Serializable(with = AdventureBossBarSerializer::class) BossBar

object AdventureBossBarSerializer : KSerializer<BossBar> {
    private val flagsSerializer = SetSerializer(serializer<BossBar.Flag>())

    override val descriptor = buildClassSerialDescriptor("surf.api.AdventureBossBar") {
        element("name", AdventureComponentSerializer.descriptor)
        element<Float>("progress")
        element<BossBar.Color>("color")
        element<BossBar.Overlay>("overlay")
        element("flags", flagsSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: BossBar,
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, AdventureComponentSerializer, value.name())
        encodeFloatElement(descriptor, 1, value.progress())
        encodeSerializableElement(descriptor, 2, serializer<BossBar.Color>(), value.color())
        encodeSerializableElement(descriptor, 3, serializer<BossBar.Overlay>(), value.overlay())
        encodeSerializableElement(descriptor, 4, flagsSerializer, value.flags())
    }


    override fun deserialize(decoder: Decoder): BossBar = decoder.decodeStructure(descriptor) {
        var name: Component? = null
        var progress: Float? = null
        var color: BossBar.Color? = null
        var overlay: BossBar.Overlay? = null
        var flags: Set<BossBar.Flag>? = null

        if (decodeSequentially()) {
            name = decodeSerializableElement(descriptor, 0, AdventureComponentSerializer)
            progress = decodeFloatElement(descriptor, 1)
            color = decodeSerializableElement(descriptor, 2, serializer<BossBar.Color>())
            overlay = decodeSerializableElement(descriptor, 3, serializer<BossBar.Overlay>())
            flags = decodeSerializableElement(descriptor, 4, flagsSerializer)
        } else {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name =
                        decodeSerializableElement(descriptor, 0, AdventureComponentSerializer)

                    1 -> progress = decodeFloatElement(descriptor, 1)
                    2 -> color =
                        decodeSerializableElement(descriptor, 2, serializer<BossBar.Color>())

                    3 -> overlay =
                        decodeSerializableElement(descriptor, 3, serializer<BossBar.Overlay>())

                    4 -> flags = decodeSerializableElement(descriptor, 4, flagsSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        BossBar.bossBar(
            name ?: error("Missing name"),
            progress ?: error("Missing progress"),
            color ?: error("Missing color"),
            overlay ?: error("Missing overlay"),
            flags ?: emptySet(),
        )
    }
}