package dev.slne.surf.surfapi.core.api.toast

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

class ToastBuilder {
    private var icon: ItemType = ItemTypes.STONE
    private var text: SurfComponentBuilder.() -> Unit = {}
    private var style: ToastStyle = ToastStyle.TASK

    fun icon(packetType: ItemType) {
        this.icon = packetType
    }

    fun text(block: SurfComponentBuilder.() -> Unit) {
        this.text = block
    }

    fun style(style: ToastStyle) {
        this.style = style
    }

    fun build() = ToastService.INSTANCE.createToast(
        icon,
        SurfComponentBuilder().apply(text).build(),
        style
    )
}

fun toast(block: ToastBuilder.() -> Unit): Toast {
    return ToastBuilder().apply(block).build()
}

