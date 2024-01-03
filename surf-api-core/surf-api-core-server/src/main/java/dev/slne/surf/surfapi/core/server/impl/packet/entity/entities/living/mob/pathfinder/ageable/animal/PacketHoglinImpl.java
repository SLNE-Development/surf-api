package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketHoglin;

import java.util.UUID;

public final class PacketHoglinImpl extends PacketAnimalImpl<PacketHoglin> implements PacketHoglin {

    public PacketHoglinImpl(UUID uuid) {
        super(uuid, EntityTypes.HOGLIN);
    }

    @Override
    public boolean immuneToZombification() {
        return get(IMMUNE_TO_ZOMBIFICATION_INDEX, false);
    }

    @Override
    public void immuneToZombification(boolean immune) {
        set(IMMUNE_TO_ZOMBIFICATION_INDEX, immune);
        afterSet();
    }
}
