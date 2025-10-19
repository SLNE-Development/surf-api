package dev.slne.surf.surfapi.velocity.api.toast

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastService
import dev.slne.surf.surfapi.core.api.toast.ToastStyle

class ToastVelocityBuilder {
    private var icon: ItemType = ItemTypes.STONE
    private var text: SurfComponentBuilder.() -> Unit = {}
    private var style: ToastStyle = ToastStyle.TASK

    fun icon(iconType: ItemType) {
        this.icon = iconType
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

fun toast(block: ToastVelocityBuilder.() -> Unit): Toast {
    return ToastVelocityBuilder().apply(block).build()
}

