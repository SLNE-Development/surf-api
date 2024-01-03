package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketMonster;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMobImpl;

import java.util.UUID;

public abstract class PacketMonsterImpl<Impl extends PacketMonster<Impl>> extends PacketPathfinderMobImpl<Impl> implements PacketMonster<Impl> {

    public PacketMonsterImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }
}
