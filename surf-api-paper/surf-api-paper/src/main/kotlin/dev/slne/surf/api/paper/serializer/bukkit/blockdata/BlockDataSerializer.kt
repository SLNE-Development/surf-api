package dev.slne.surf.api.paper.serializer.bukkit.blockdata

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData

object BlockDataSerializer : KSerializer<BlockData> {
    override val descriptor =
        PrimitiveSerialDescriptor("surfapi.bukkit.BlockData", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BlockData,
    ) {
        encoder.encodeString(value.asString)
    }

    override fun deserialize(decoder: Decoder) =
        Bukkit.createBlockData(decoder.decodeString())
}