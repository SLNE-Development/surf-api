package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketVex;
import java.util.UUID;

public final class PacketVexImpl extends PacketMonsterImpl<PacketVex> implements PacketVex {

  public PacketVexImpl(UUID uuid) {
    super(uuid, EntityTypes.VEX);
  }

  @Override
  public boolean charging() {
    return getMaskBit(VEX_FLAGS_ID, CHARGING_FLAG);
  }

  @Override
  public void charging(boolean charging) {
    setMaskBit(VEX_FLAGS_ID, CHARGING_FLAG, charging);
    afterSet();
  }
}
