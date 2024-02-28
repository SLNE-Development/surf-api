package dev.slne.surf.surfapi.bukkit.server.impl.packet.entity;

import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection;
import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;

public class BukkitEntityIdProvider implements EntityIdProvider {

  @Override
  public int nextEntityId() {
    return Reflection.ENTITY_PROXY.generateEntityId();
  }
}
