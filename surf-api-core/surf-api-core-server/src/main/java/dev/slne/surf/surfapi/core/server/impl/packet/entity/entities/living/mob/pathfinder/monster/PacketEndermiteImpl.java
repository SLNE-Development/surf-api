package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketEndermite;

import java.util.UUID;

public final class PacketEndermiteImpl extends PacketMonsterImpl<PacketEndermite> implements PacketEndermite {

    public PacketEndermiteImpl(UUID uuid) {
        super(uuid, EntityTypes.ENDERMITE);
    }
}
