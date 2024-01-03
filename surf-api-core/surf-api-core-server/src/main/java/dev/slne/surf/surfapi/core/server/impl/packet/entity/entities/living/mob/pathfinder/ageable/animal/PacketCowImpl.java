package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketCow;

import java.util.UUID;

public sealed class PacketCowImpl<Impl extends PacketCow<Impl>> extends PacketAnimalImpl<Impl> implements PacketCow<Impl> permits PacketMooshroomCowImpl {

    public PacketCowImpl(UUID uuid) {
        super(uuid, EntityTypes.COW);
    }

    protected PacketCowImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
