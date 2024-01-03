package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimal;

public interface PacketAbstractHorse<Impl extends PacketAbstractHorse<Impl>> extends PacketAnimal<Impl> {

    int ABSTRACT_HORSE_BIT_MASK_INDEX = 17;

    byte TAMED_BIT = 0x02, SADDLED_BIT = 0x04, HAS_BREAD_BIT = 0x08, EATING_BIT = 0x10, REARING_BIT = 0x20,
            MOUTH_OPEN_BIT = 0x40;

    boolean tamed();

    void tamed(boolean tamed);

    boolean saddled();

    void saddled(boolean saddled);

    boolean hasBread();

    void hasBread(boolean hasBread);

    boolean eating();

    void eating(boolean eating);

    boolean rearing();

    void rearing(boolean rearing);

    boolean mouthOpen();

    void mouthOpen(boolean mouthOpen);
}
