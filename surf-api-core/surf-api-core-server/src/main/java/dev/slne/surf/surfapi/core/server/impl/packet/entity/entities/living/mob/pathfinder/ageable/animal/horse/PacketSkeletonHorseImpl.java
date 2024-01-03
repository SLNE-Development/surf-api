package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketSkeletonHorse;

import java.util.UUID;

public final class PacketSkeletonHorseImpl extends PacketAbstractHorseImpl<PacketSkeletonHorse> implements PacketSkeletonHorse {

    public PacketSkeletonHorseImpl(UUID uuid) {
        super(uuid, EntityTypes.SKELETON_HORSE);
    }
}
