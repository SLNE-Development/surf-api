package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketElderGuardian;

import java.util.UUID;

public final class PacketElderGuardianImpl extends PacketGuardianImpl<PacketElderGuardian> implements PacketElderGuardian {
    public PacketElderGuardianImpl(UUID uuid) {
        super(uuid, EntityTypes.ELDER_GUARDIAN);
    }
}
