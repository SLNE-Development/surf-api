package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.adventure.component.shadowcolor.AdventureShadowColorSerializer
import kotlinx.serialization.protobuf.ProtoIntegerType
import net.kyori.adventure.text.format.ShadowColor

/**
 * Serializes an Adventure [ShadowColor] as the protobuf message `surf.types.v1.ShadowColor`.
 *
 * Field `argb` (1, required, `fixed32`) holds `0xAARRGGBB` as written by
 * [AdventureShadowColorSerializer]. Every 32-bit value is a valid color, including fully
 * transparent `0`. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to
 * [ShadowColor] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `argb` is absent
 */
object ShadowColorSerializer : ProtoWrapperSerializer<ShadowColor>(
    "surf.types.v1.ShadowColor",
    AdventureShadowColorSerializer,
    fieldName = "argb",
    integerType = ProtoIntegerType.FIXED,
)
