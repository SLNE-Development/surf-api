package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketHusk;
import java.util.UUID;

public final class PacketHuskImpl extends PacketZombieImpl<PacketHusk> implements PacketHusk {

  public PacketHuskImpl(UUID uuid) {
    super(uuid, EntityTypes.HUSK);
  }
}
