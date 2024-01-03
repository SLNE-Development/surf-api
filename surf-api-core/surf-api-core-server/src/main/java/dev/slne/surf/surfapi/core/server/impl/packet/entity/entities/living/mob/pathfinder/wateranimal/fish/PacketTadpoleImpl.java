package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketTadpole;

import java.util.UUID;

public final class PacketTadpoleImpl extends PacketAbstractFishImpl<PacketTadpole> implements PacketTadpole {

    public PacketTadpoleImpl(UUID uuid) {
        super(uuid, EntityTypes.TADPOLE);
    }
}
