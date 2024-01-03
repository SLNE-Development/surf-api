package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketMooshroomCow extends PacketCow<PacketMooshroomCow>, Spawnable {

    int VARIANT_INDEX = 17;

    /**
     * Get the variant of this cow.
     *
     * @return cow variant
     */
    Variant variant();

    /**
     * Set the variant of this cow.
     *
     * @param variant cow variant
     */
    void variant(@NotNull Variant variant);

    /**
     * Represents the variant of a cow - ie its color.
     */
    enum Variant {
        /**
         * Red mushroom cow.
         */
        RED("red"),
        /**
         * Brown mushroom cow.
         */
        BROWN("brown");

        public static final Object2ObjectMap<String, Variant> BY_ID = Util.byStringIdMap(Variant.class, Variant::getId);
        private final String id;

        @Contract(pure = true)
        Variant(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
