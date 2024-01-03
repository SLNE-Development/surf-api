package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketCamel;

import java.util.UUID;

public final class PacketCamelImpl extends PacketAbstractHorseImpl<PacketCamel> implements PacketCamel {

    public PacketCamelImpl(UUID uuid) {
        super(uuid, EntityTypes.CAMEL);
    }

    @Override
    public boolean dashing() {
        return get(DASHING_INDEX, false);
    }

    @Override
    public void dashing(boolean dashing) {
        set(DASHING_INDEX, dashing);
        afterSet();
    }

    @Override
    public int lastPoseChangeTime() {
        return get(DASHING_INDEX, 0);
    }

    @Override
    public void lastPoseChangeTime(int lastPoseChangeTime) {
        set(DASHING_INDEX, lastPoseChangeTime);
        afterSet();
    }
}
