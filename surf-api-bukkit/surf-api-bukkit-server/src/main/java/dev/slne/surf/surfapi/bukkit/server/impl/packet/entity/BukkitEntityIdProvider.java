package dev.slne.surf.surfapi.bukkit.server.impl.packet.entity;

import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import org.bukkit.Bukkit;

public class BukkitEntityIdProvider implements EntityIdProvider {

  @Override
  @SuppressWarnings("deprecation") // We are safe to use this method
  public int nextEntityId() {
    return Bukkit.getUnsafe().nextEntityId();
  }
}
