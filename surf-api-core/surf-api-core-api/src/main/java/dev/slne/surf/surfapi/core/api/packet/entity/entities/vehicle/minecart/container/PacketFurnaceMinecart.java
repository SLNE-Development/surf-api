package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketFurnaceMinecart extends PacketAbstractMinecartContainer<PacketFurnaceMinecart>, Spawnable {

    int HAS_FUEL_INDEX = 14;

    /**
     * @return true if the furnace minecart has fuel
     */
    boolean hasFuel();

    /**
     * @param hasFuel true if the furnace minecart has fuel
     */
    default void hasFuel(boolean hasFuel) {
        hasFuel(hasFuel, true);
    }

    /**
     * @param hasFuel                   true if the furnace minecart has fuel
     * @param updateDisplayedBlockState true if the displayed block state should be updated (normale furnace / lit furnace)
     */
    void hasFuel(boolean hasFuel, boolean updateDisplayedBlockState);
}
