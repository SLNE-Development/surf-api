package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketBee;

import java.util.UUID;

public final class PacketBeeImpl extends PacketAnimalImpl<PacketBee> implements PacketBee {

    public PacketBeeImpl(UUID uuid) {
        super(uuid, EntityTypes.BEE);
    }

    @Override
    public boolean angry() {
        return getMaskBit(BEE_FLAGS_INDEX, ANGRY_FLAG);
    }

    @Override
    public void angry(boolean angry) {
        setMaskBit(BEE_FLAGS_INDEX, ANGRY_FLAG, angry);
        afterSet();
    }

    @Override
    public boolean hasStung() {
        return getMaskBit(BEE_FLAGS_INDEX, HAS_STUNG_FLAG);
    }

    @Override
    public void hasStung(boolean hasStung) {
        setMaskBit(BEE_FLAGS_INDEX, HAS_STUNG_FLAG, hasStung);
        afterSet();
    }

    @Override
    public boolean hasNectar() {
        return getMaskBit(BEE_FLAGS_INDEX, HAS_NECTAR_FLAG);
    }

    @Override
    public void hasNectar(boolean hasNectar) {
        setMaskBit(BEE_FLAGS_INDEX, HAS_NECTAR_FLAG, hasNectar);
        afterSet();
    }

    @Override
    public int angerTime() {
        return get(ANGER_TIME_INDEX, 0);
    }

    @Override
    public void angerTime(int angerTime) {
        set(ANGER_TIME_INDEX, angerTime);
        afterSet();
    }
}
