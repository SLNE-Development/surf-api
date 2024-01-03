package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@CanBeSpawned
public interface PacketEnderman extends PacketMonster<PacketEnderman>, Spawnable {

    int CARRIED_BLOCK_INDEX = 16, SCREAMING_INDEX = 17, STARED_AT_INDEX = 18;

    /**
     * Gets the data of the block that the Enderman is carrying.
     *
     * @return BlockData containing the carried block, or null if none
     */
    Optional<WrappedBlockState> carriedBlock();

    /**
     * Sets the data of the block that the Enderman is carrying.
     *
     * @param blockData data to set the carried block to, or null to remove
     */
    void carriedBlock(@Nullable WrappedBlockState blockData);

    /**
     * Returns whether the enderman is screaming/angry.
     *
     * @return whether the enderman is screaming
     */
    boolean screaming();

    /**
     * Sets whether the enderman is screaming/angry.
     *
     * @param screaming whether the enderman is screaming
     */
    void screaming(boolean screaming);

    /**
     * Returns whether the enderman has been stared at.
     * If set to true, players will hear an ambient sound.
     *
     * @return whether the enderman has been stared at
     */
    boolean hasBeenStaredAt();

    /**
     * Sets whether the enderman has been stared at.
     * If set to true, players will hear an ambient sound.
     *
     * @param hasBeenStaredAt whether the enderman has been stared at
     */
    void hasBeenStaredAt(boolean hasBeenStaredAt);
}
