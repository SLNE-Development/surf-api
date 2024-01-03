package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketPolarBear extends PacketAnimal<PacketPolarBear>, Spawnable {

    int STANDING_INDEX = 17;

    /**
     * Returns whether the polar bear is standing.
     *
     * @return whether the polar bear is standing
     */
    boolean standing();

    /**
     * Sets whether the polar bear is standing.
     *
     * @param standing whether the polar bear should be standing
     */
    void standing(boolean standing);
}
