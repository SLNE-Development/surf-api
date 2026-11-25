package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.java.number.bigdecimal.BigDecimalStringSerializer
import java.math.BigDecimal

/**
 * Serializes [BigDecimal] as the protobuf message `surf.types.v1.BigDecimal`.
 *
 * Field `value` (1, required) holds [BigDecimal.toString], for example `"12.50"` or `"1E+3"`, as
 * written by [BigDecimalStringSerializer]. Unscaled value and scale are preserved, so `12.50` and
 * `12.5` stay distinct. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to
 * [BigDecimal] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `value` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `value` is not a decimal
 */
object BigDecimalSerializer : ProtoWrapperSerializer<BigDecimal>(
    "surf.types.v1.BigDecimal",
    BigDecimalStringSerializer,
)
