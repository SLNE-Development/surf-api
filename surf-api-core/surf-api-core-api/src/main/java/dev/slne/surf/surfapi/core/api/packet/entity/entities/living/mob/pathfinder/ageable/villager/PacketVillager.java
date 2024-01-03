package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfession;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerType;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@CanBeSpawned
public interface PacketVillager extends PacketAbstractVillager<PacketVillager>, Spawnable {

    int VILLAGER_DATA_INDEX = 18;

    /**
     * Gets the current type of this villager.
     *
     * @return Current type.
     */
    VillagerType villagerType();

    /**
     * Sets the new type of this villager.
     *
     * @param type New type.
     */
    void villagerType(@NotNull VillagerType type);

    /**
     * Gets the current profession of this villager.
     *
     * @return Current profession.
     */
    VillagerProfession profession();

    /**
     * Sets the new profession of this villager.
     *
     * @param profession New profession.
     */
    void profession(@NotNull VillagerProfession profession);

    /**
     * Gets the level of this villager.
     * <p>
     * A villager with a level of 1 and no experience is liable to lose its
     * profession.
     *
     * @return this villager's level
     */
    @Range(from = 1, to = 5)
    int villagerLevel();

    /**
     * Sets the level of this villager.
     * <p>
     * A villager with a level of 1 and no experience is liable to lose its
     * profession.
     * <p>
     * This doesn't update the trades of this villager.
     *
     * @param level the new level
     * @throws IllegalArgumentException if level not between [1, 5]
     */
    void villagerLevel(@Range(from = 1, to = 5) int level);
}
