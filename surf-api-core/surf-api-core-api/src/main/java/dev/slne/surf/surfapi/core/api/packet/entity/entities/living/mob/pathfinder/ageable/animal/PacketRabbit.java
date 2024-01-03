package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketRabbit extends PacketAnimal<PacketRabbit>, Spawnable {

    int RABBIT_TYPE_INDEX = 17;

    /**
     * @return The type of rabbit.
     */
    Type rabbitType();

    /**
     * @param type Sets the type of rabbit for this entity.
     */
    void setRabbitType(@NotNull Type type);

    /**
     * Represents the various types a Rabbit might be.
     */
    enum Type {

        /**
         * Chocolate colored rabbit.
         */
        BROWN,
        /**
         * Pure white rabbit.
         */
        WHITE,
        /**
         * Black rabbit.
         */
        BLACK,
        /**
         * Black with white patches, or white with black patches?
         */
        BLACK_AND_WHITE,
        /**
         * Golden bunny.
         */
        GOLD,
        /**
         * Salt and pepper colored, whatever that means.
         */
        SALT_AND_PEPPER,
        /**
         * Rabbit with pure white fur, blood red horizontal eyes, and is hostile to players.
         */
        THE_KILLER_BUNNY
    }
}
