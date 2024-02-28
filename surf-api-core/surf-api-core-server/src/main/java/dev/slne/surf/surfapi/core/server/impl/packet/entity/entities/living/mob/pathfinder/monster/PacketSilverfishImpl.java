package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketSilverfish;
import java.util.UUID;

public final class PacketSilverfishImpl extends PacketMonsterImpl<PacketSilverfish> implements
    PacketSilverfish {

  public PacketSilverfishImpl(UUID uuid) {
    super(uuid, EntityTypes.SILVERFISH);
  }
}
