package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.skeleton;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.skeleton.PacketWitherSkeleton;
import java.util.UUID;

public final class PacketWitherSkeletonImpl extends
    PacketAbstractSkeletonImpl<PacketWitherSkeleton> implements PacketWitherSkeleton {

  public PacketWitherSkeletonImpl(UUID uuid) {
    super(uuid, EntityTypes.WITHER_SKELETON);
  }
}
