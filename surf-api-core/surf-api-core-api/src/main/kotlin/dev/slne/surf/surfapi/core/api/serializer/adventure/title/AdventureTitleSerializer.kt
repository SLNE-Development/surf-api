@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.adventure.title

import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

typealias SerializableTitle = @Serializable(with = AdventureTitleSerializer::class) Title
typealias SerializableTitleTimes = @Serializable(with = AdventureTitleSerializer.AdventureTitleTimes::class) Title.Times

object AdventureTitleSerializer : KSerializer<Title> {
    override val descriptor = buildClassSerialDescriptor("surfapi.Title") {
        element("title", AdventureComponentSerializer.descriptor)
        element("subtitle", AdventureComponentSerializer.descriptor)
        element("times", AdventureTitleTimes.descriptor, isOptional = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: Title,
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(
            descriptor,
            0,
            AdventureComponentSerializer,
            value.title()
        )
        encodeSerializableElement(
            descriptor,
            1,
            AdventureComponentSerializer,
            value.subtitle()
        )
        encodeNullableSerializableElement(
            descriptor,
            2,
            AdventureTitleTimes,
            value.times()
        )
    }

    override fun deserialize(decoder: Decoder): Title = decoder.decodeStructure(descriptor) {
        var title: Component? = null
        var subtitle: Component? = null
        var times: Title.Times? = null

        if (decodeSequentially()) {
            title = decodeSerializableElement(
                descriptor,
                0,
                AdventureComponentSerializer
            )
            subtitle = decodeSerializableElement(
                descriptor,
                1,
                AdventureComponentSerializer
            )
            times = decodeNullableSerializableElement(
                descriptor,
                2,
                AdventureTitleTimes
            )
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> title = decodeSerializableElement(
                    descriptor,
                    0,
                    AdventureComponentSerializer
                )

                1 -> subtitle = decodeSerializableElement(
                    descriptor,
                    1,
                    AdventureComponentSerializer
                )

                2 -> times = decodeNullableSerializableElement(
                    descriptor,
                    2,
                    AdventureTitleTimes
                )

                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        Title.title(
            title ?: error("Title cannot be null"),
            subtitle ?: error("Subtitle cannot be null"),
            times
        )
    }

    object AdventureTitleTimes : KSerializer<Title.Times> {
        override val descriptor = buildClassSerialDescriptor("surfapi.Title.Times") {
            element<Duration>("fade_in")
            element<Duration>("stay")
            element<Duration>("fade_out")
        }

        override fun serialize(
            encoder: Encoder,
            value: Title.Times,
        ) = encoder.encodeStructure(descriptor) {
            encodeSerializableElement(
                descriptor,
                0,
                Duration.serializer(),
                value.fadeIn().toKotlinDuration()
            )
            encodeSerializableElement(
                descriptor,
                1,
                Duration.serializer(),
                value.stay().toKotlinDuration()
            )
            encodeSerializableElement(
                descriptor,
                2,
                Duration.serializer(),
                value.fadeOut().toKotlinDuration()
            )
        }

        override fun deserialize(
            decoder: Decoder,
        ): Title.Times = decoder.decodeStructure(descriptor) {
            var fadeIn: Duration = Duration.ZERO
            var stay: Duration = Duration.ZERO
            var fadeOut: Duration = Duration.ZERO

            if (decodeSequentially()) {
                fadeIn = decodeSerializableElement(descriptor, 0, Duration.serializer())
                stay = decodeSerializableElement(descriptor, 1, Duration.serializer())
                fadeOut = decodeSerializableElement(descriptor, 2, Duration.serializer())
            } else while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> fadeIn =
                        decodeSerializableElement(descriptor, 0, Duration.serializer())

                    1 -> stay = decodeSerializableElement(descriptor, 1, Duration.serializer())
                    2 -> fadeOut =
                        decodeSerializableElement(descriptor, 2, Duration.serializer())

                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            Title.Times.times(
                fadeIn.toJavaDuration(),
                stay.toJavaDuration(),
                fadeOut.toJavaDuration()
            )
        }
    }
}