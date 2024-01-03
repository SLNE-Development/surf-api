package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketGoat extends PacketAnimal<PacketGoat>, Spawnable {

    int SCREAMING_INDEX = 17, LEFT_HORN_INDEX = 18, RIGHT_HORN_INDEX = 19;

    /**
     * Gets if this is a screaming goat.
     * <p>
     * A screaming goat makes screaming sounds and rams more often. They do not
     * offer home loans.
     *
     * @return screaming status
     */
    boolean screaming();

    /**
     * Sets if this is a screaming goat.
     * <p>
     * A screaming goat makes screaming sounds and rams more often. They do not
     * offer home loans.
     *
     * @param screaming screaming status
     */
    void screaming(boolean screaming);

    /**
     * Gets if this goat has its left horn.
     *
     * @return left horn status
     */
    boolean leftHorn();

    /**
     * Sets if this goat has its left horn.
     *
     * @param hasHorn left horn status
     */
    void leftHorn(boolean hasHorn);

    /**
     * Gets if this goat has its right horn.
     *
     * @return right horn status
     */
    boolean rightHorn();

    /**
     * Sets if this goat has its right horn.
     *
     * @param hasHorn right horn status
     */
    void rightHorn(boolean hasHorn);
}
