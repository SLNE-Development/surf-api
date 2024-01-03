package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfession;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerType;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketZombieVillager extends PacketZombie<PacketZombieVillager>, Spawnable {

    int CONVERTING_INDEX = 19, VILLAGER_DATA_INDEX = 20;

    /**
     * Get if this entity is in the process of converting to a Villager as a
     * result of being cured.
     *
     * @return conversion status
     */
    boolean converting();

    /**
     * Set if this entity is in the process of converting to a Villager as a
     * result of being cured.
     *
     * @param converting conversion status
     */
    void converting(boolean converting);

    /**
     * Gets the current type of this villager.
     *
     * @return Current type.
     */
    @NotNull
    VillagerType villagerType();

    /**
     * Sets the new type of this villager.
     *
     * @param type New type.
     */
    void villagerType(@NotNull VillagerType type);

    /**
     * Returns the villager profession of this zombie.
     *
     * @return the profession
     */
    VillagerProfession villagerProfession();

    /**
     * Sets the villager profession of this zombie.
     *
     * @param profession the profession
     */
    void villagerProfession(@NotNull VillagerProfession profession);

    /**
     * Returns the villager level of this zombie.
     *
     * @return the level
     */
    int villagerLevel();

    /**
     * Sets the villager level of this zombie.
     *
     * @param level the level
     */
    void villagerLevel(int level);
}
