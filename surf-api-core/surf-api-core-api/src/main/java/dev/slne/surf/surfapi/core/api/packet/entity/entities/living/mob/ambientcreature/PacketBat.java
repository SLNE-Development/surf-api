package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.ambientcreature;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketBat extends PacketAmbientCreature<PacketBat>, Spawnable {

    int BAT_BIT_MASK_INDEX = 16;

    byte HANGING_BIT = 0x01;

    boolean hanging();

    void hanging(boolean hanging);
}
