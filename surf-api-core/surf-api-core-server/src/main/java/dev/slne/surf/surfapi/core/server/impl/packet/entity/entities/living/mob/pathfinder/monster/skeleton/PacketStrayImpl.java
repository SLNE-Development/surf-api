package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.skeleton;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.skeleton.PacketStray;
import java.util.UUID;

public final class PacketStrayImpl extends PacketAbstractSkeletonImpl<PacketStray> implements
    PacketStray {

  public PacketStrayImpl(UUID uuid) {
    super(uuid, EntityTypes.STRAY);
  }
}
