package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.flying;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.flying.PacketGhast;
import java.util.UUID;

public final class PacketGhastImpl extends PacketFlyingImpl<PacketGhast> implements PacketGhast {

  public PacketGhastImpl(UUID uuid) {
    super(uuid, EntityTypes.GHAST);
  }

  @Override
  public boolean charging() {
    return get(CHARGING_INDEX, false);
  }

  @Override
  public void charging(boolean charging) {
    set(CHARGING_INDEX, charging);
    afterSet();
  }
}
