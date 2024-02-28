package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Optional;

/**
 * Represents a llama wich can be spawned.
 */
@CanBeSpawned
public interface PacketLlama<Impl extends PacketLlama<Impl>> extends PacketChestedHorse<Impl>, Spawnable {

    /**
     * The indexer for the llama
     */
    int STRENGTH_INDEX = 19, CARPET_COLOR_INDEX = 20, COLOR_INDEX = 21;

    /**
     * Gets the llama's strength. A higher strength llama will have more
     * inventory slots and be more threatening to entities.
     *
     * @return llama strength [1..5]
     */
    @Useless
    @Range(from = 1, to = 5)
    int strength();

    /**
     * Sets the llama's strength. A higher strength llama will have more
     * inventory slots and be more threatening to entities. Inventory slots are
     * equal to strength * 3.
     *
     * @param strength llama strength [1..5]
     */
    @Useless
    void strength(@Range(from = 1, to = 5) int strength);

    /**
     * Gets the llama's carpet color.
     *
     * @return the llama's carpet color
     */
    Optional<DyeColor> carpetColor();

    /**
     * Sets the llama's carpet color.
     *
     * @param carpetColor the llama's carpet color
     */
    void carpetColor(@Nullable DyeColor carpetColor);

    /**
     * Gets the llama's base color.
     *
     * @return the llama's base color
     */
    Color color();

    /**
     * Sets the llama's base color.
     *
     * @param color the llama's base color
     */
    void color(@NotNull Color color);

    /**
     * Represents the base color that the llama has.
     */
    enum Color {

        /**
         * A cream-colored llama.
         */
        CREAMY,
        /**
         * A white llama.
         */
        WHITE,
        /**
         * A brown llama.
         */
        BROWN,
        /**
         * A gray llama.
         */
        GRAY
    }
}
