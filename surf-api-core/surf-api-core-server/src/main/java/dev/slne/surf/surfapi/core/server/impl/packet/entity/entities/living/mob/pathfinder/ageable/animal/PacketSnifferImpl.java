package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.sniffer.SnifferState;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketSniffer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketSnifferImpl extends PacketAnimalImpl<PacketSniffer> implements PacketSniffer {

    public PacketSnifferImpl(UUID uuid) {
        super(uuid, EntityTypes.SNIFFER);
    }

    @Override
    public SnifferState state() {
        return get(STATE_INDEX, SnifferState.IDLING);
    }

    @Override
    public void state(@NotNull SnifferState state) {
        set(STATE_INDEX, EntityDataTypes.SNIFFER_STATE, state);
        afterSet();
    }

    @Override
    public int dropSeedAtTick() {
        return get(DROP_SEED_AT_TICK_INDEX, 0);
    }

    @Override
    public void dropSeedAtTick(int dropSeedAtTick) {
        set(DROP_SEED_AT_TICK_INDEX, dropSeedAtTick);
        afterSet();
    }
}
