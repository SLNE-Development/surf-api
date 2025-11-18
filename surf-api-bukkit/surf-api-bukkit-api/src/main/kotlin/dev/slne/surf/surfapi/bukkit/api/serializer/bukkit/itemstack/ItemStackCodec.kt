package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.itemstack

import com.mojang.serialization.Codec
import org.bukkit.inventory.ItemStack
import java.nio.ByteBuffer

object ItemStackCodec {
    val CODEC: Codec<ItemStack> = Codec.BYTE_BUFFER
        .xmap(
            { buffer ->
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                ItemStack.deserializeBytes(bytes)
            },
            { itemStack -> ByteBuffer.wrap(itemStack.serializeAsBytes()) }
        )
}