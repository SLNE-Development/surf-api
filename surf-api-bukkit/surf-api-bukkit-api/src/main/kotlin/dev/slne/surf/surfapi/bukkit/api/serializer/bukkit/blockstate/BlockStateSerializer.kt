package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.blockstate

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.block.BlockState

object BlockStateSerializer : KSerializer<BlockState> {
    override val descriptor =
        PrimitiveSerialDescriptor("surfapi.bukkit.BlockState", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BlockState,
    ) {
        encoder.encodeString(value.blockData.asString)
    }

    override fun deserialize(decoder: Decoder) =
        Bukkit.createBlockData(decoder.decodeString()).createBlockState()
}