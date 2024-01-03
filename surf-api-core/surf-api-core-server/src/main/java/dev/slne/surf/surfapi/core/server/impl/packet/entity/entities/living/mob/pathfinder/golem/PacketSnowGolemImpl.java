package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.golem;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem.PacketSnowGolem;

import java.util.UUID;

public final class PacketSnowGolemImpl extends PacketAbstractGolemImpl<PacketSnowGolem> implements PacketSnowGolem {

    public PacketSnowGolemImpl(UUID uuid) {
        super(uuid, EntityTypes.SNOW_GOLEM);
    }

    @Override
    public boolean derp() {
        return !getMaskBit(SNOW_GOLEM_FLAG_INDEX, SNOW_GOLEM_FLAG);
    }

    @Override
    public void derp(boolean derpMode) {
        setMask(SNOW_GOLEM_FLAG_INDEX, derpMode ? 0 : SNOW_GOLEM_FLAG);
        afterSet();
    }
}
