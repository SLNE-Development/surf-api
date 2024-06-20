package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets;

import org.bukkit.entity.Player;

public interface PacketOperation {

  void execute(Player player);

  PacketOperation add(PacketOperation operation);
}
