package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketZombieHorse;

import java.util.UUID;

public final class PacketZombieHorseImpl extends PacketAbstractHorseImpl<PacketZombieHorse> implements PacketZombieHorse {

    public PacketZombieHorseImpl(UUID uuid) {
        super(uuid, EntityTypes.ZOMBIE_HORSE);
    }
}
