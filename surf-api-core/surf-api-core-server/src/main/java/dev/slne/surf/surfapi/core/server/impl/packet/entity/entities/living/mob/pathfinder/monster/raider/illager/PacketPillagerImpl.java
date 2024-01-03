package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketPillager;

import java.util.UUID;

public final class PacketPillagerImpl extends PacketAbstractIllagerImpl<PacketPillager> implements PacketPillager {

    public PacketPillagerImpl(UUID uuid ) {
        super(uuid, EntityTypes.PILLAGER);
    }

    @Override
    public boolean chargingCrossbow() {
        return get(CHARGING_INDEX, false);
    }

    @Override
    public void chargingCrossbow(boolean chargingCrossbow) {
        set(CHARGING_INDEX, chargingCrossbow);
        afterSet();
    }
}
