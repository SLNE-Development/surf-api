package dev.slne.surf.surfapi.bukkit.api.toast

import dev.slne.surf.surfapi.bukkit.api.surfBukkitApi
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import org.bukkit.Material

class ToastBuilder {
    private var icon: Material = Material.STONE
    private var text: SurfComponentBuilder.() -> Unit = {}
    private var style: ToastStyle = ToastStyle.TASK

    fun icon(material: Material) {
        this.icon = material
    }

    fun text(block: SurfComponentBuilder.() -> Unit) {
        this.text = block
    }

    fun style(style: ToastStyle) {
        this.style = style
    }

    fun build() =
        surfBukkitApi.createToast(
            icon,
            SurfComponentBuilder().apply(text).build(),
            style
        )
}

fun toast(block: ToastBuilder.() -> Unit): Toast {
    return ToastBuilder().apply(block).build()
}

