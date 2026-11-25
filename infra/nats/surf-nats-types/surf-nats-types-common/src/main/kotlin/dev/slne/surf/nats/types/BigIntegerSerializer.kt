package dev.slne.surf.nats.types

import java.math.BigInteger
import dev.slne.surf.api.serialization.java.number.bigint.BigIntegerSerializer as SurfBigIntegerSerializer

/**
 * Serializes [BigInteger] as the protobuf message `surf.types.v1.BigInteger`.
 *
 * Field `value` (1, required) holds the decimal digits with an optional leading `-`, as written by
 * [SurfBigIntegerSerializer]. The `dev.slne.surf.nats` Gradle plugin maps fields of this message
 * type to [BigInteger] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `value` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `value` is not a decimal
 *   integer
 */
object BigIntegerSerializer : ProtoWrapperSerializer<BigInteger>(
    "surf.types.v1.BigInteger",
    SurfBigIntegerSerializer,
)
