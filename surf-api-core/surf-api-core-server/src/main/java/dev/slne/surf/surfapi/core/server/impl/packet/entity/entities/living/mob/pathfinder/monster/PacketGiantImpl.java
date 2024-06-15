package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketGiant;
import java.util.UUID;

public final class PacketGiantImpl extends PacketMonsterImpl<PacketGiant> implements PacketGiant {

  public PacketGiantImpl(UUID uuid) {
    super(uuid, EntityTypes.GIANT);
  }
}
