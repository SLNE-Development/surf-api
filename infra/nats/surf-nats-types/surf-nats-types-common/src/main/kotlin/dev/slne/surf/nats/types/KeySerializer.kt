package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.adventure.key.AdventureKeySerializer
import net.kyori.adventure.key.Key

/**
 * Serializes an Adventure [Key] as the protobuf message `surf.types.v1.Key`.
 *
 * Field `value` (1, required) holds `namespace:value` as written by [AdventureKeySerializer]. A
 * value without namespace decodes in the `minecraft` namespace. The `dev.slne.surf.nats` Gradle
 * plugin maps fields of this message type to [Key] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `value` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `value` is not a valid key
 */
object KeySerializer : ProtoWrapperSerializer<Key>("surf.types.v1.Key", AdventureKeySerializer)
