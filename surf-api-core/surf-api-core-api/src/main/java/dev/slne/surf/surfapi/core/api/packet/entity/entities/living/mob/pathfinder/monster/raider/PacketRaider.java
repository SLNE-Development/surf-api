package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketMonster;

public interface PacketRaider<Impl extends PacketRaider<Impl>> extends PacketMonster<Impl> {

    int CELEBRATING_INDEX = 16;

    /**
     * Check whether or not this raider is celebrating a raid victory.
     *
     * @return true if celebrating, false otherwise
     */
    boolean celebrating();

    /**
     * Set whether or not this mob is celebrating a raid victory.
     *
     * @param celebrating whether or not to celebrate
     */
    void celebrating(boolean celebrating);
}
