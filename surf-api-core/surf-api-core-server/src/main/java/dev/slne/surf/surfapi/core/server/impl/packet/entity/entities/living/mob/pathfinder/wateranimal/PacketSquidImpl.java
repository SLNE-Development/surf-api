package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketSquid;

import java.util.UUID;

public sealed class PacketSquidImpl<Impl extends PacketSquid<Impl>> extends PacketWaterAnimalImpl<Impl> implements PacketSquid<Impl> permits PacketGlowSquidImpl {

    public PacketSquidImpl(UUID uuid) {
        super(uuid, EntityTypes.SQUID);
    }

    protected PacketSquidImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
