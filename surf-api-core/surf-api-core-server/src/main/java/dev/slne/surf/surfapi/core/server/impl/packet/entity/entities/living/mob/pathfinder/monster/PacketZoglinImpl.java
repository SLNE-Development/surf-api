package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketZoglin;

import java.util.UUID;

public final class PacketZoglinImpl extends PacketMonsterImpl<PacketZoglin> implements
    PacketZoglin {

  public PacketZoglinImpl(UUID uuid) {
    super(uuid, EntityTypes.ZOGLIN);
  }

  @Override
  public boolean baby() {
    return get(BABY_INDEX, false);
  }

  @Override
  public void baby(boolean baby) {
    set(BABY_INDEX, baby);
    afterSet();
  }
}
