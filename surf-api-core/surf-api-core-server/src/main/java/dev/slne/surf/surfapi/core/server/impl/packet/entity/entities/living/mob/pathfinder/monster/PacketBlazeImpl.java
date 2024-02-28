package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketBlaze;
import java.util.UUID;

public final class PacketBlazeImpl extends PacketMonsterImpl<PacketBlaze> implements PacketBlaze {

  public PacketBlazeImpl(UUID uuid) {
    super(uuid, EntityTypes.BLAZE);
  }

  @Override
  public boolean charged() {
    return getMaskBit(BLAZE_FLAGS_INDEX, IS_ON_FIRE_FLAG);
  }

  @Override
  public void charged(boolean charged) {
    setMaskBit(BLAZE_FLAGS_INDEX, IS_ON_FIRE_FLAG, charged);
    afterSet();
  }
}
