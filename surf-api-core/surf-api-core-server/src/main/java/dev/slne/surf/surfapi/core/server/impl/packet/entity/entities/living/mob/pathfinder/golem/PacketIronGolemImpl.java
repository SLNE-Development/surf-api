package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.golem;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem.PacketIronGolem;
import java.util.UUID;

public final class PacketIronGolemImpl extends PacketAbstractGolemImpl<PacketIronGolem> implements
    PacketIronGolem {

  public PacketIronGolemImpl(UUID uuid) {
    super(uuid, EntityTypes.IRON_GOLEM);
  }

  @Override
  public boolean playerCreated() {
    return getMaskBit(IRON_GOLEM_FLAGS_INDEX, PLAYER_CREATED_FLAG);
  }

  @Override
  public void playerCreated(boolean playerCreated) {
    setMaskBit(IRON_GOLEM_FLAGS_INDEX, PLAYER_CREATED_FLAG, playerCreated);
    afterSet();
  }
}
