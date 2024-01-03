package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketCat extends PacketTameableAnimal<PacketCat>, Spawnable {

    int CAT_TYPE = 19, LYING_DOWN = 20, HEAD_UP = 21, COLLAR_COLOR = 22;

    /**
     * Gets the current type of this cat.
     *
     * @return Type of the cat.
     */
    Type catType();

    /**
     * Sets the current type of this cat.
     *
     * @param type New type of this cat.
     */
    void catType(@NotNull Type type);

    /**
     * Gets if the cat is lying down.
     *
     * @return whether the cat is lying down
     */
    boolean lyingDown();

    /**
     * Sets if the cat is lying down.
     *
     * @param lyingDown whether the cat should lie down
     */
    void lyingDown(boolean lyingDown);

    /**
     * Gets if the cat has its head up.
     *
     * @return head is up
     */
    boolean headUp();

    /**
     * Sets if the cat has its head up.
     *
     * @param headUp head is up
     */
    void headUp(boolean headUp);

    /**
     * Get the collar color of this cat
     *
     * @return the color of the collar
     */
    DyeColor collarColor();

    /**
     * Set the collar color of this cat
     *
     * @param color the color to apply
     */
    void collarColor(@NotNull DyeColor color);

    /**
     * Represents the various different cat types there are.
     */
    enum Type {
        TABBY,
        BLACK,
        RED,
        SIAMESE,
        BRITISH_SHORTHAIR,
        CALICO,
        PERSIAN,
        RAGDOLL,
        WHITE,
        JELLIE,
        ALL_BLACK;

        public static final Int2ObjectMap<Type> BY_ID = Util.byIdMap(Type.class, Type::id);

        public int id() {
            return ordinal();
        }
    }
}
