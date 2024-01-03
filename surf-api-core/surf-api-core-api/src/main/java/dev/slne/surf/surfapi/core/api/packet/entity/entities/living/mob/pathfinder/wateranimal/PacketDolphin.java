package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3i;

@CanBeSpawned
public interface PacketDolphin extends PacketWaterAnimal<PacketDolphin>, Spawnable {

    int TREASURE_POSITION_INDEX = 16, HAS_FISH_INDEX = 17, MOISTNESS_LEVEL_INDEX = 18;

    Vector3i treasurePosition();

    void treasurePosition(@NotNull Vector3i treasurePosition);

    boolean hasFish();

    void hasFish(boolean hasFish);

    int moistnessLevel();

    void moistnessLevel(int moistnessLevel);
}
