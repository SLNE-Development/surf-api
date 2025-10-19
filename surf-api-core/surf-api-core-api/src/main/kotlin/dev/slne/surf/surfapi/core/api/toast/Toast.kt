package dev.slne.surf.surfapi.core.api.toast

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import net.kyori.adventure.text.Component
import java.util.*

interface Toast {
    val icon: ItemType
    val text: Component
    val style: ToastStyle

    fun send(player: UUID)
}
