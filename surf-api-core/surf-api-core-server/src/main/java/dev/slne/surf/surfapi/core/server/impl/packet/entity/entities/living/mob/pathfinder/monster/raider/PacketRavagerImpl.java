package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketRavager;

import java.util.UUID;

public final class PacketRavagerImpl extends PacketRaiderImpl<PacketRavager> implements PacketRavager {

    public PacketRavagerImpl(UUID uuid) {
        super(uuid, EntityTypes.RAVAGER);
    }
}
