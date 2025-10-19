package dev.slne.surf.surfapi.core.api.toast

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.text.Component
import java.util.*

interface ToastService {
    fun createToast(icon: ItemType, text: Component, style: ToastStyle): Toast
    fun sendToast(player: UUID, toast: Toast)

    companion object {
        val INSTANCE = requiredService<ToastService>()
    }
}