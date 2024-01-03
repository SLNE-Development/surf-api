package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketWolf extends PacketTameableAnimal<PacketWolf>, Spawnable {

    int INTERESTED_INDEX = 19, COLLAR_COLOR_INDEX = 20, ANGER_TIME_INDEX = 21;

    /**
     * Gets if the wolf is interested
     *
     * @return Whether the wolf is interested
     */
    boolean interested();

    /**
     * Set wolf to be interested
     *
     * @param interested Whether the wolf is interested
     */
    void interested(boolean interested);

    /**
     * Get the collar color of this wolf
     *
     * @return the color of the collar
     */
    DyeColor collarColor();

    /**
     * Set the collar color of this wolf
     *
     * @param color the color to apply
     */
    void collarColor(@NotNull DyeColor color);

    /**
     * Gets the remaining time of the wolf's anger
     *
     * @return The remaining time of the wolf's anger
     */
    int remainingAngerTime();

    /**
     * Sets the remaining time of the wolf's anger
     *
     * @param remainingAngerTime The remaining time of the wolf's anger
     */
    void remainingAngerTime(int remainingAngerTime);
}
