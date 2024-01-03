package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

@CanBeSpawned
public interface PacketFrog extends PacketAnimal<PacketFrog>, Spawnable {

    int VARIANT_INDEX = 18, TONGUE_TARGET_INDEX = 19;

    /**
     * Get the variant of this frog.
     *
     * @return frog variant
     */
    Variant variant();

    /**
     * Set the variant of this frog.
     *
     * @param variant frog variant
     */
    void variant(@NotNull Variant variant);

    /**
     * Gets the tongue target entity id of this frog.
     *
     * @return tongue target or empty if none
     */
    OptionalInt tongueTargetId();

    /**
     * Sets the tongue target entity id of this frog.
     *
     * @param id tongue target or null to clear
     */
    void tongueTargetId(@Nullable Integer id);

    /**
     * Represents the variant of a frog - ie its color.
     */
    enum Variant {

        /**
         * Temperate (brown-orange) frog.
         */
        TEMPERATE,
        /**
         * Warm (gray) frog.
         */
        WARM,
        /**
         * Cold (green) frog.
         */
        COLD
    }
}
