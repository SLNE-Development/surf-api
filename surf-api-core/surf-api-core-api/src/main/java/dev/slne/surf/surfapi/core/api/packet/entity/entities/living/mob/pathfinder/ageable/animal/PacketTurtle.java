package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3i;

@CanBeSpawned
public interface PacketTurtle extends PacketAnimal<PacketTurtle>, Spawnable {

    /**
     * Indexes for the turtle's data
     */
    int HOME_POS_INDEX = 17, HAS_EGG_INDEX = 18, LAYING_EGG_INDEX = 19, TRAVEL_POS_INDEX = 20, GOING_HOME_INDEX = 21,
            TRAVELING_INDEX = 22;

    /**
     * Get the turtle's home location
     *
     * @return Home location
     */
    Vector3i homePos();

    /**
     * Set the turtle's home location
     *
     * @param pos Home location
     */
    void homePos(@NotNull Vector3i pos);

    /**
     * Gets whether the turtle has an egg
     *
     * @return Whether the turtle has an egg
     */
    boolean hasEgg();

    /**
     * Set if turtle is carrying egg
     *
     * @param hasEgg True if carrying egg
     */
    void hasEgg(boolean hasEgg);

    /**
     * Gets whether the turtle is laying an egg
     *
     * @return Whether the turtle is laying an egg
     */
    boolean layingEgg();

    /**
     * Set if turtle is laying an egg
     *
     * @param layingEgg True if laying an egg
     */
    void layingEgg(boolean layingEgg);

    /**
     * Get the turtle's travel location
     *
     * @return Travel location
     */
    Vector3i travelPos();

    /**
     * Set the turtle's travel location
     *
     * @param pos Travel location
     */
    void travelPos(@NotNull Vector3i pos);

    /**
     * Check if turtle is currently pathfinding to it's home
     *
     * @return True if going home
     */
    boolean goingHome();

    /**
     * Set if turtle is currently pathfinding to it's home
     *
     * @param goingHome True if going home
     */
    void goingHome(boolean goingHome);

    /**
     * Check if turtle is currently traveling
     *
     * @return True if traveling
     */
    boolean traveling();

    /**
     * Set if turtle is currently traveling
     *
     * @param traveling True if traveling
     */
    void traveling(boolean traveling);
}
