package dev.slne.surf.surfapi.core.server.impl.toast

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastService
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.Services
import java.util.*

@AutoService(ToastService::class)
class ToastServiceImpl : ToastService, Services.Fallback {
    override fun createToast(
        icon: ItemType,
        text: Component,
        style: ToastStyle
    ) = ToastImpl(
        icon,
        text,
        style
    )

    override fun sendToast(
        player: UUID,
        toast: Toast
    ) = toast.send(player)
}