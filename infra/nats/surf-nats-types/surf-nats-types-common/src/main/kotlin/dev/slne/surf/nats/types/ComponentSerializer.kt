package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.adventure.component.AdventureComponentSerializer
import net.kyori.adventure.text.Component

/**
 * Serializes an Adventure [Component] as the protobuf message `surf.types.v1.Component`.
 *
 * Field `json` (1, required) holds the JSON written by [AdventureComponentSerializer]. The
 * component is compacted with [Component.compact] before encoding, so the decoded component renders
 * the same but can differ structurally: an empty text component whose only child is
 * `Component.text("a")` decodes as `Component.text("a")`. The `dev.slne.surf.nats` Gradle plugin
 * maps fields of this message type to [Component] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `json` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `json` is not a valid
 *   component
 */
object ComponentSerializer : ProtoWrapperSerializer<Component>(
    "surf.types.v1.Component",
    AdventureComponentSerializer,
    fieldName = "json",
)
