package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.skeleton;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.skeleton.PacketSkeleton;

import java.util.UUID;

public final class PacketSkeletonImpl extends PacketAbstractSkeletonImpl<PacketSkeleton> implements PacketSkeleton {

    public PacketSkeletonImpl(UUID uuid) {
        super(uuid, EntityTypes.SKELETON);
    }
}
