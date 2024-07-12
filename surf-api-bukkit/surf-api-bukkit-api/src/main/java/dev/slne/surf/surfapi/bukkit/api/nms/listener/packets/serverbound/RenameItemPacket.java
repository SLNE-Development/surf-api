package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public non-sealed interface RenameItemPacket extends NmsServerboundPacket {

  String getNewName();
}
