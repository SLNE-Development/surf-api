package dev.slne.surf.surfapi.core.api.util

import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import org.jetbrains.annotations.ApiStatus
import java.util.function.Consumer

@ApiStatus.NonExtendable
@Deprecated("Not longer maintained.")
interface ItemStackFactory {
    companion object {
        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "ItemStack.builder()\n" +
                        ".type(material)\n" +
                        ".amount(amount)\n" +
                        ".nbt(NBTCompound().also { nbtConsumer.accept(it) })\n" +
                        ".build()",
                "java.util.function.Consumer"
            )
        )
        @JvmOverloads
        fun of(
            material: ItemType, amount: Int = 1, nbtConsumer: Consumer<NBTCompound> = Consumer { }
        ): ItemStack {
            val nbt = NBTCompound()
            nbtConsumer.accept(nbt)

            return ItemStack.builder().type(material).amount(amount).nbt(NBTCompound().also { nbtConsumer.accept(it) })
                .build()
        }
    }
}
