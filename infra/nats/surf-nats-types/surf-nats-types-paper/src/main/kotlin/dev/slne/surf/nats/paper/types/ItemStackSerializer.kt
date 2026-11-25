package dev.slne.surf.nats.paper.types

import dev.slne.surf.nats.types.ProtoWrapperSerializer
import org.bukkit.inventory.ItemStack
import dev.slne.surf.api.paper.serializer.bukkit.itemstack.ItemStackSerializer as PaperItemStackSerializer

/**
 * Serializes a Paper [ItemStack] as the protobuf message `surf.types.paper.v1.ItemStack`.
 *
 * Field `data` (1, required) holds the bytes of [ItemStack.serializeAsBytes], including the data
 * version, as written by [PaperItemStackSerializer]. Decoding uses [ItemStack.deserializeBytes],
 * which migrates items written by an older Minecraft version. The bytes are in the server's own
 * item format, so encoding and decoding need a running server. In Paper projects the
 * `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [ItemStack] with this
 * serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `data` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `data` is empty or not a
 *   valid serialized item stack
 */
object ItemStackSerializer : ProtoWrapperSerializer<ItemStack>(
    "surf.types.paper.v1.ItemStack",
    PaperItemStackSerializer,
    fieldName = "data",
)
