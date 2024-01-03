package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.golem;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem.PacketAbstractGolem;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMobImpl;

import java.util.UUID;

public abstract class PacketAbstractGolemImpl<Impl extends PacketAbstractGolem<Impl>> extends PacketPathfinderMobImpl<Impl> implements PacketAbstractGolem<Impl> {

    public PacketAbstractGolemImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
