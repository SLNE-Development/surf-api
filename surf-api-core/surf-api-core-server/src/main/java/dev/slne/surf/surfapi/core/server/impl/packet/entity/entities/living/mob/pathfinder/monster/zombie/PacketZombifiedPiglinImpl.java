package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketZombifiedPiglin;

import java.util.UUID;

public final class PacketZombifiedPiglinImpl extends PacketZombieImpl<PacketZombifiedPiglin> implements PacketZombifiedPiglin {

    public PacketZombifiedPiglinImpl(UUID uuid) {
        super(uuid, EntityTypes.ZOMBIFIED_PIGLIN);
    }
}
