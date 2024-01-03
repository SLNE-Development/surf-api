package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketSheep extends PacketAnimal<PacketSheep>, Spawnable {

    /**
     * The index of the sheep's flag.
     */
    int SHEEP_FLAG_INDEX = 17;

    /**
     * The flags of the sheep.
     */
    byte COLOR_FLAG = 0x0F, SHEARED_FLAG = 0x10;

    /**
     * @return The color of the sheep.
     */
    DyeColor sheepColor();

    /**
     * @param color The color of the sheep.
     */
    void sheepColor(@NotNull DyeColor color);

    /**
     * @return Whether the sheep is sheared.
     */
    boolean sheared();

    /**
     * @param sheared Whether the sheep is sheared.
     */
    void sheared(boolean sheared);
}
