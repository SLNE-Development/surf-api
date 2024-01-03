package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimal;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.PacketAgeableMobImpl;

import java.util.UUID;

public abstract class PacketAnimalImpl<Impl extends PacketAnimal<Impl>> extends PacketAgeableMobImpl<Impl> implements PacketAnimal<Impl> {

    public PacketAnimalImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
