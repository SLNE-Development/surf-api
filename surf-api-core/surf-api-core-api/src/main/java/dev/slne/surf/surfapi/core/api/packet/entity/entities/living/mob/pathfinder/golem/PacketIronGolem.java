package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketIronGolem extends PacketAbstractGolem<PacketIronGolem>, Spawnable {

    int IRON_GOLEM_FLAGS_INDEX = 16;

    byte PLAYER_CREATED_FLAG = 0x01;

    /**
     * Gets whether this iron golem was built by a player.
     *
     * @return Whether this iron golem was built by a player
     */
    boolean playerCreated();

    /**
     * Sets whether this iron golem was built by a player or not.
     *
     * @param playerCreated true if you want to set the iron golem as being
     *                      player created, false if you want it to be a natural village golem.
     */
    void playerCreated(boolean playerCreated);
}
