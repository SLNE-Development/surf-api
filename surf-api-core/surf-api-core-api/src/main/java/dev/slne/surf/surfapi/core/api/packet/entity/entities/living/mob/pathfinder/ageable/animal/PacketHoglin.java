package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketHoglin extends PacketAnimal<PacketHoglin>, Spawnable {

    int IMMUNE_TO_ZOMBIFICATION_INDEX = 17;

    /**
     * Gets whether the hoglin is immune to zombification.
     *
     * @return Whether the hoglin is immune to zombification
     */
    boolean immuneToZombification();

    /**
     * Sets whether the hoglin is immune to zombification.
     *
     * @param immune Whether the hoglin is immune to zombification
     */
    void immuneToZombification(boolean immune);
}
