package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketDrowned;

import java.util.UUID;

public final class PacketDrownedImpl extends PacketZombieImpl<PacketDrowned> implements PacketDrowned {
    public PacketDrownedImpl(UUID uuid) {
        super(uuid, EntityTypes.DROWNED);
    }
}
