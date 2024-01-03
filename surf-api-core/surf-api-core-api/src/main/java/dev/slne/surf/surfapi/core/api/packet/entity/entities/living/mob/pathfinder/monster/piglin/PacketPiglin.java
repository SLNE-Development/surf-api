package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.piglin;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketPiglin extends PacketBasePiglin<PacketPiglin>, Spawnable {

    /**
     * The indexes for the piglin
     */
    int BABY_INDEX = 16, CHARGING_CROSSBOW_INDEX = 17, DANCING_INDEX = 18;

    /**
     * Gets whether the piglin is a baby
     *
     * @return Whether the piglin is a baby
     */
    boolean baby();

    /**
     * Sets whether the piglin is a baby
     *
     * @param flag Whether the piglin is a baby
     */
    void baby(boolean flag);

    /**
     * Gets if the piglin is currently charging the
     * item in their hand.
     *
     * @return is charging
     */
    boolean chargingCrossbow();

    /**
     * Causes the piglin to appear as if they are charging
     * a crossbow.
     * <p>
     * This works with any item currently held in the piglin's hand.
     *
     * @param chargingCrossbow is charging
     */
    void chargingCrossbow(boolean chargingCrossbow);

    /**
     * Gets if the piglin is currently dancing
     *
     * @return is dancing
     */
    boolean dancing();

    /**
     * Sets whether the Piglin is dancing or not
     *
     * @param dancing is dancing
     */
    void dancing(boolean dancing);

}
