package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketOcelot;

import java.util.UUID;

public final class PacketOcelotImpl extends PacketAnimalImpl<PacketOcelot> implements PacketOcelot {

    public PacketOcelotImpl(UUID uuid) {
        super(uuid, EntityTypes.OCELOT);
    }

    @Override
    public boolean trusting() {
        return get(TRUSTING_INDEX, false);
    }

    @Override
    public void trusting(boolean trusting) {
        set(TRUSTING_INDEX, trusting);
        afterSet();
    }
}
