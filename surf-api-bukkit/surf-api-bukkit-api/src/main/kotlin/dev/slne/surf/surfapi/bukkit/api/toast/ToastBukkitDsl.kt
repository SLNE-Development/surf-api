package dev.slne.surf.surfapi.bukkit.api.toast

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastService
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Material

class ToastBukkitBuilder {
    private var icon: Material = Material.STONE
    private var text: SurfComponentBuilder.() -> Unit = {}
    private var style: ToastStyle = ToastStyle.TASK

    fun icon(iconMaterial: Material) {
        this.icon = iconMaterial
    }

    fun text(block: SurfComponentBuilder.() -> Unit) {
        this.text = block
    }

    fun style(style: ToastStyle) {
        this.style = style
    }

    fun build() = ToastService.INSTANCE.createToast(
        SpigotConversionUtil.fromBukkitItemMaterial(icon),
        SurfComponentBuilder().apply(text).build(),
        style
    )
}

fun toast(block: ToastBukkitBuilder.() -> Unit): Toast {
    return ToastBukkitBuilder().apply(block).build()
}

