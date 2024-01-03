package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketMule;

import java.util.UUID;

public final class PacketMuleImpl extends PacketChestedHorseImpl<PacketMule> implements PacketMule {
    public PacketMuleImpl(UUID uuid) {
        super(uuid, EntityTypes.MULE);
    }
}
