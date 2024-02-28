package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.skeleton;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.skeleton.PacketAbstractSkeleton;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.PacketMonsterImpl;
import java.util.UUID;

public abstract class PacketAbstractSkeletonImpl<Impl extends PacketAbstractSkeleton<Impl>> extends
    PacketMonsterImpl<Impl> implements PacketAbstractSkeleton<Impl> {

  public PacketAbstractSkeletonImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }
}
