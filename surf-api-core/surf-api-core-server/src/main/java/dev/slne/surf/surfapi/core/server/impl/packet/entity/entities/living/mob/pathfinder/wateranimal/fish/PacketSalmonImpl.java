package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketSalmon;

import java.util.UUID;

public final class PacketSalmonImpl extends PacketAbstractFishImpl<PacketSalmon> implements PacketSalmon {

    public PacketSalmonImpl(UUID uuid) {
        super(uuid, EntityTypes.SALMON);
    }
}
