package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket

@NmsUseWithCaution
sealed interface NmsServerboundPacket : NmsPacket
