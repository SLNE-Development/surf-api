package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface NmsServerboundPacket extends NmsPacket permits RenameItemPacket,
    SignUpdatePacket {

}
