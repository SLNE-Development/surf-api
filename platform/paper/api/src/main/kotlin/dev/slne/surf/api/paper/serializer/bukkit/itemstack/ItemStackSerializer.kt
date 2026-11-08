package dev.slne.surf.api.paper.serializer.bukkit.itemstack

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.ItemStack

object ItemStackSerializer : KSerializer<ItemStack> {
    private val arraySerializer = ByteArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.bukkit.ItemStack", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: ItemStack,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.serializeAsBytes())
    }

    override fun deserialize(decoder: Decoder) =
        ItemStack.deserializeBytes(decoder.decodeSerializableValue(arraySerializer))
}