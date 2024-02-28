package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketWarden;
import java.util.UUID;

public final class PacketWardenImpl extends PacketMonsterImpl<PacketWarden> implements
    PacketWarden {

  public PacketWardenImpl(UUID uuid) {
    super(uuid, EntityTypes.WARDEN);
  }

  @Override
  public int anger() {
    return get(ANGER_INDEX, 0);
  }

  @Override
  public void anger(int anger) {
    checkArgument(anger >= 0 && anger <= 150, "anger must be between 0 and 150");

    set(ANGER_INDEX, anger);
    afterSet();
  }
}
