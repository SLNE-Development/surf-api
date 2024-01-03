package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllager;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketRaiderImpl;

import java.util.UUID;

public abstract class PacketAbstractIllagerImpl<Impl extends PacketAbstractIllager<Impl>> extends PacketRaiderImpl<Impl> implements PacketAbstractIllager<Impl> {

    public PacketAbstractIllagerImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
