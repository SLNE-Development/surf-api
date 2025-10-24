package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.toast

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.toastPacketsBridge
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import io.papermc.paper.advancement.AdvancementDisplay
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

data class Toast(
    val icon: ItemStack,
    val title: Component,
    val frame: AdvancementDisplay.Frame = AdvancementDisplay.Frame.TASK,
) {

    @NmsUseWithCaution
    fun createOperation() = toastPacketsBridge.showToast(this)

    companion object {
        inline operator fun invoke(block: Builder.() -> Unit): Toast {
            return Builder().apply(block).build()
        }

        class Builder @PublishedApi internal constructor() {
            private var icon: ItemStack? = null
            private var title: Component? = null
            private var frame: AdvancementDisplay.Frame = AdvancementDisplay.Frame.TASK

            fun icon(itemStack: ItemStack) {
                this.icon = itemStack
            }

            fun icon(type: ItemType) {
                this.icon = type.createItemStack()
            }

            fun icon(type: ItemType, block: ItemStack.() -> Unit) {
                this.icon = buildItem(type, init = block)
            }

            fun title(component: Component) {
                this.title = component
            }

            fun title(block: SurfComponentBuilder.() -> Unit) {
                this.title = SurfComponentBuilder(block)
            }

            fun frame(frame: AdvancementDisplay.Frame) {
                this.frame = frame
            }

            fun build() = Toast(
                icon = icon ?: error("Icon must be set!"),
                title = title ?: error("Title must be set!"),
                frame = frame
            )
        }
    }
}

inline fun toast(block: Toast.Companion.Builder.() -> Unit): Toast {
    return Toast(block)
}
