package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketAbstractHorse;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimalImpl;

import java.util.UUID;

public abstract class PacketAbstractHorseImpl<Impl extends PacketAbstractHorse<Impl>> extends PacketAnimalImpl<Impl> implements PacketAbstractHorse<Impl> {

    public PacketAbstractHorseImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public boolean tamed() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, TAMED_BIT);
    }

    @Override
    public void tamed(boolean tamed) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, TAMED_BIT, tamed);
        afterSet();
    }

    @Override
    public boolean saddled() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, SADDLED_BIT);
    }

    @Override
    public void saddled(boolean saddled) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, SADDLED_BIT, saddled);
        afterSet();
    }

    @Override
    public boolean hasBread() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, HAS_BREAD_BIT);
    }

    @Override
    public void hasBread(boolean hasBread) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, HAS_BREAD_BIT, hasBread);
        afterSet();
    }

    @Override
    public boolean eating() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, EATING_BIT);
    }

    @Override
    public void eating(boolean eating) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, EATING_BIT, eating);
        afterSet();
    }

    @Override
    public boolean rearing() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, REARING_BIT);
    }

    @Override
    public void rearing(boolean rearing) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, REARING_BIT, rearing);
        afterSet();
    }

    @Override
    public boolean mouthOpen() {
        return getMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, MOUTH_OPEN_BIT);
    }

    @Override
    public void mouthOpen(boolean mouthOpen) {
        setMaskBit(ABSTRACT_HORSE_BIT_MASK_INDEX, MOUTH_OPEN_BIT, mouthOpen);
        afterSet();
    }
}
