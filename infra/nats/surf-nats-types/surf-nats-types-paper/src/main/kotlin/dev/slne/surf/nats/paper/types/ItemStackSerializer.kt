package dev.slne.surf.nats.paper.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import org.bukkit.inventory.ItemStack

/**
 * Serializes a Paper [ItemStack] as the protobuf message `surf.types.paper.v1.ItemStack`.
 *
 * Field `data` (1, required) holds the NBT bytes of [ItemStack.serializeAsBytes], including the data version. Decoding
 * uses [ItemStack.deserializeBytes], which migrates items written by an older Minecraft version. The bytes are in the
 * server's own item format, so encoding and decoding need a running server. In Paper projects the
 * `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [ItemStack] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `data` is absent and
 * [kotlinx.serialization.SerializationException] when `data` is empty or not a valid serialized item stack.
 */
object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = ItemStackProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ItemStack) {
        encoder.encodeSerializableValue(ItemStackProto.serializer(), ItemStackProto(value.serializeAsBytes()))
    }

    override fun deserialize(decoder: Decoder): ItemStack {
        val data = decoder.decodeSerializableValue(ItemStackProto.serializer()).data
            ?: throw MissingFieldException("data", descriptor.serialName)

        return try {
            ItemStack.deserializeBytes(data)
        } catch (e: RuntimeException) {
            throw SerializationException("${descriptor.serialName}.data is not a valid serialized item stack", e)
        }
    }
}

@Serializable
@SerialName("surf.types.paper.v1.ItemStack")
private class ItemStackProto(
    @ProtoNumber(1) val data: ByteArray? = null,
)
