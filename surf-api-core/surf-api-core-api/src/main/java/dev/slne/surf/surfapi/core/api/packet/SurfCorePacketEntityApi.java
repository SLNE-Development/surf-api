package dev.slne.surf.surfapi.core.api.packet;

import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.other.PacketExperienceOrb;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import dev.slne.surf.surfapi.core.api.packet.events.GameEvent;
import dev.slne.surf.surfapi.core.api.packet.events.WorldEvent;
import net.kyori.adventure.key.Key;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface SurfCorePacketEntityApi { // TODO: look at, equipment

    void idProvider(@NotNull EntityIdProvider provider);

    @NotNull
    EntityIdProvider idProvider();

    <T extends PacketEntity<T>> void registerInteractHandler(SurfInteractHandler<T> handler);

    <T extends PacketEntity<T> & Spawnable> T spawnEntity(@NotNull Class<T> entityClass, @NotNull UUID uuid, @Nullable Consumer<T> initializer);

    default <T extends PacketEntity<T> & Spawnable> T spawnEntity(@NotNull Class<T> entityClass, @NotNull UUID uuid) {
        return spawnEntity(entityClass, uuid, null);
    }

    /**
     * Retrieves the PacketEntity with the specified UUID.
     *
     * @param uuid The UUID of the entity to retrieve.
     * @param <T>  The type of the entity's metadata.
     * @return An Optional containing the PacketEntity if an entity with the specified UUID exists, otherwise empty.
     */
    <T extends PacketEntity<T>> Optional<T> getEntity(@NotNull UUID uuid);

    /**
     * Retrieves the PacketEntity with the specified UUID (same as {@link #getEntity(UUID)} but with a class parameter
     * wich can sometimes be useful).
     *
     * @param uuid        The UUID of the entity to retrieve.
     * @param entityClass The class of the entity to retrieve.
     * @param <T>         The type of the packet entity.
     * @return An Optional containing the PacketEntity if an entity with the specified UUID exists, otherwise empty.
     */
    <T extends PacketEntity<T>> Optional<T> getEntity(@NotNull UUID uuid, Class<T> entityClass);

    /**
     * Retrieves the SurfEntity with the specified entityId.
     *
     * @param entityId the ID of the entity
     * @param <T>      the type of the entity's metadata
     * @return an Optional representing the SurfEntity with the specified entityId, or an empty Optional if no entity is found
     */
    <T extends PacketEntity<T>> Optional<T> getEntity(@Range(from = 0L, to = Integer.MAX_VALUE) int entityId);

    /**
     * Retrieves the SurfEntity with the specified entityId (same as {@link #getEntity(int)} but with a class parameter
     * wich can sometimes be useful).
     *
     * @param entityId    the ID of the entity
     * @param entityClass the class of the entity
     * @param <T>         the type of the entity's metadata
     * @return an Optional representing the SurfEntity with the specified entityId, or an empty Optional if no entity is found
     */
    <T extends PacketEntity<T>> Optional<T> getEntity(@Range(from = 0L, to = Integer.MAX_VALUE) int entityId, Class<T> entityClass);

    /**
     * Deletes the specified entity.
     * <p>
     * This method will despawn the entity and remove it from the entity registry.
     * Any calls to any methods on the entity after this method will end up in undefined behavior.
     * </p>
     *
     * @param entity The entity to delete.
     * @param <T>    The type of the entity.
     */
    <T extends PacketEntity<T>> void deleteEntity(T entity);

    PacketExperienceOrb spawnExperienceOrb(@NotNull Vector3i position, short experience, UUID @NotNull ... viewers);

    void spawnExplosion(@NotNull Vector3d position,
                        @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                        @Nullable List<Vector3i> relativeAffectedBlocks,
                        @Nullable Vector3f playerVelocity,
                        @Nullable Particle smallExplosionParticle,
                        @Nullable Particle largeExplosionParticle,
                        @Nullable WrapperPlayServerExplosion.BlockInteraction blockAction,
                        @Nullable Key explosionSound,
                        @Nullable Float explosionSoundRange,
                        @ArrayLenRange(from = 1) UUID @NotNull ... viewers);

    default void spawnExplosion(@NotNull Vector3d position,
                                @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                                @Nullable List<Vector3i> relativeAffectedBlocks,
                                @Nullable Vector3f playerVelocity,
                                @Nullable WrapperPlayServerExplosion.BlockInteraction blockAction,
                                @Nullable Key explosionSound,
                                @Nullable Float explosionSoundRange,
                                @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        spawnExplosion(position, power, relativeAffectedBlocks, playerVelocity, null, null, blockAction, explosionSound, explosionSoundRange, viewers);
    }

    default void spawnExplosion(@NotNull Vector3d position,
                                @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                                @Nullable List<Vector3i> relativeAffectedBlocks,
                                @Nullable Vector3f playerVelocity,
                                @Nullable Key explosionSound,
                                @Nullable Float explosionSoundRange,
                                @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        spawnExplosion(position, power, relativeAffectedBlocks, playerVelocity, null, null, null, explosionSound, explosionSoundRange, viewers);
    }

    default void spawnExplosion(@NotNull Vector3d position,
                                @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                                @Nullable List<Vector3i> relativeAffectedBlocks,
                                @Nullable Vector3f playerVelocity,
                                @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        spawnExplosion(position, power, relativeAffectedBlocks, playerVelocity, null, null, null, null, null, viewers);
    }

    default void spawnExplosion(@NotNull Vector3d position,
                                @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                                @Nullable List<Vector3i> relativeAffectedBlocks,
                                @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        spawnExplosion(position, power, relativeAffectedBlocks, null, null, null, null, null, null, viewers);
    }

    default void spawnExplosion(@NotNull Vector3d position,
                                @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                                @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        spawnExplosion(position, power, null, null, null, null, null, null, null, viewers);
    }

    /**
     * Used for a wide variety of game events, from weather to bed use to game mode to demo messages.
     *
     * @param type    the type of game event
     * @param value   the value of the game event (can be null -
     *                check if the type field is annotated with
     *                {@link dev.slne.surf.surfapi.core.api.packet.events.GameEvent.ValueCanBeNull})
     * @param viewers the viewers of the game event
     * @param <T>     the type of the game event
     */
    <T> void gameEvent(GameEvent.Type<T> type, @CheckForNull T value, UUID @NotNull ... viewers);

    void playHurtAnimation(int entityId, float yaw,  UUID @NotNull ... viewers);

    <T> void worldEvent(WorldEvent.Type<T> type, Vector3i position, @CheckForNull T value, UUID @NotNull ... viewers);

    <T> void worldEvent(WorldEvent.AllowBooleanType<T> type, Vector3i position, @CheckForNull T value, boolean global, UUID @NotNull ... viewers);

    default void worldEvent(WorldEvent.SimpleType type, Vector3i position, UUID @NotNull ... viewers) {
        worldEvent(type, position, null, viewers);
    }

    default<T> void worldEvent(WorldEvent.AllowBooleanType<T> type, Vector3i position, boolean global, UUID @NotNull ... viewers) {
        worldEvent(type, position, null, global, viewers);
    }



    static SurfCorePacketEntityApi get() {
        return SurfCorePacketApi.get().getPacketEntityApi();
    }
}
