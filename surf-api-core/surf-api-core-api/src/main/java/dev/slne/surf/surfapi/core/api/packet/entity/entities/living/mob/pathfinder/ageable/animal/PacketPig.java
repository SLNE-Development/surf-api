package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.Range;

@CanBeSpawned
public interface PacketPig extends PacketAnimal<PacketPig>, Spawnable {

    int SADDLE_INDEX = 17, BOOST_TICKS_INDEX = 18;

    /**
     * Check if the pig has a saddle.
     *
     * @return if the pig has been saddled.
     */
    boolean hasSaddle();

    /**
     * Sets if the pig has a saddle or not
     *
     * @param saddled set if the pig has a saddle or not.
     */
    void hasSaddle(boolean saddled);

    /**
     * Get the time in ticks this entity's movement is being increased.
     *
     * @return the current boost ticks
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int boostTicks();

    /**
     * Set the time in ticks this entity's movement will be increased.
     *
     * @param ticks the boost time
     */
    void boostTicks(@Range(from = 0, to = Integer.MAX_VALUE) int ticks);
}
