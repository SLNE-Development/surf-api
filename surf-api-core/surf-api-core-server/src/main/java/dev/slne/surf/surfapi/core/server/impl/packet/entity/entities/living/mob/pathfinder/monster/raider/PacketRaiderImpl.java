package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketRaider;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.PacketMonsterImpl;

import java.util.UUID;

public abstract class PacketRaiderImpl<Impl extends PacketRaider<Impl>> extends PacketMonsterImpl<Impl> implements PacketRaider<Impl> {

    public PacketRaiderImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public boolean celebrating() {
        return get(CELEBRATING_INDEX, false);
    }

    @Override
    public void celebrating(boolean celebrating) {
        set(CELEBRATING_INDEX, celebrating);
        afterSet();
    }
}
