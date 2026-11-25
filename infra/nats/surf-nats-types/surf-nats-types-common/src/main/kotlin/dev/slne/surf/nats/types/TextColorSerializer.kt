package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.adventure.component.textcolor.AdventureTextColorSerializer
import net.kyori.adventure.text.format.TextColor

/**
 * Serializes an Adventure [TextColor] as the protobuf message `surf.types.v1.TextColor`.
 *
 * Field `rgb` (1, required) holds `0xRRGGBB`, 0 to 16,777,215, as written by
 * [AdventureTextColorSerializer]. A value that matches a named color decodes as the
 * [net.kyori.adventure.text.format.NamedTextColor] constant. The `dev.slne.surf.nats` Gradle plugin
 * maps fields of this message type to [TextColor] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `rgb` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `rgb` is out of range
 */
object TextColorSerializer : ProtoWrapperSerializer<TextColor>(
    "surf.types.v1.TextColor",
    AdventureTextColorSerializer,
    fieldName = "rgb",
)
