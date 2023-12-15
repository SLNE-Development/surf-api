package dev.slne.surf.surfapi.bukkit.api.packet.entity;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfLivingEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.interact.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface SurfBukkitPacketEntityApi {

    /**
     * Registers a SurfBukkitInteractListener to listen for interact events.
     *
     * @param listener the SurfBukkitInteractListener to register
     */
    void registerInteractListener(@NotNull SurfBukkitInteractListener listener);

    /**
     * Registers a SurfBukkitInteractListener to listen for interact events on a specific entity.
     *
     * @param entity   the UUID of the entity
     * @param listener the SurfBukkitInteractListener to register
     */
    void registerInteractListener(@NotNull UUID entity, @NotNull SurfBukkitInteractListener listener);

    void registerInteractListener(@NotNull UUID entity, @NotNull Collection<SurfBukkitInteractListener> listener);

    /**
     * Unregisters a SurfBukkitInteractListener.
     *
     * @param listener the SurfBukkitInteractListener to unregister
     */
    void unregisterInteractListener(@NotNull SurfBukkitInteractListener listener);

    /**
     * Unregisters a SurfBukkitInteractListener from a specific entity.
     *
     * @param entity the UUID of the entity
     */
    void unregisterInteractListener(@NotNull UUID entity);

    /**
     * Unregisters a SurfBukkitInteractListener from a specific entity.
     *
     * @param entity the UUID of the entity
     */
    void unregisterInteractListener(@NotNull UUID entity, @NotNull SurfBukkitInteractListener listener);

    /**
     * Unregisters a SurfBukkitInteractListener from a specific entity.
     *
     * @param entity the UUID of the entity
     */
    void unregisterInteractListener(@NotNull UUID entity, @NotNull Collection<SurfBukkitInteractListener> listener);

    /**
     * Creates an entity with the given UUID and type.
     *
     * @param uuid the UUID of the entity (must be unique)
     * @param type the type of the entity
     * @param <M>  the type of the entity's metadata
     * @return a SurfEntity object representing the created entity
     */
    @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid,
                                                               @NotNull EntityType.EntityType0<M> type);

    @NotNull <M extends LivingEntityMeta> SurfLivingEntity<M> createEntity(@NotNull UUID uuid,
                                                                           @NotNull EntityType.LivingEntityType<M> type);

    /**
     * Creates a SurfEntity with the given UUID, type, and metadata modifier.
     *
     * @param uuid       the UUID of the entity (must be unique)
     * @param type       the type of the entity
     * @param changeMeta a Consumer function to modify the metadata of the entity
     * @param <M>        the type of the entity's metadata
     * @return a SurfEntity object representing the created entity
     */
    @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid,
                                                               @NotNull EntityType.EntityType0<M> type,
                                                               @NotNull Consumer<M> changeMeta);

    @NotNull <M extends LivingEntityMeta> SurfLivingEntity<M> createEntity(@NotNull UUID uuid,
                                                                           @NotNull EntityType.LivingEntityType<M> type,
                                                                           @NotNull Consumer<M> changeMeta);

    void deleteEntity(@NotNull UUID uuid);

    /**
     * Retrieves the SurfEntity with the specified UUID.
     *
     * @param uuid The UUID of the entity to retrieve.
     * @param <T>  The type of the entity's metadata.
     * @return An Optional containing the SurfEntity if an entity with the specified UUID exists, otherwise empty.
     */
    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid);

    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid,
                                                             @NotNull EntityType.EntityType0<T> type);

    <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@NotNull UUID uuid);

    <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@NotNull UUID uuid,
                                                                         @NotNull EntityType.LivingEntityType<T> type);

    /**
     * Retrieves the SurfEntity with the specified entityId.
     *
     * @param entityId the ID of the entity
     * @param <T>      the type of the entity's metadata
     * @return an Optional representing the SurfEntity with the specified entityId, or an empty Optional if no entity is found
     */
    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@Range(from = 0L, to = Long.MAX_VALUE) int entityId);

    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@Range(from = 0L, to = Long.MAX_VALUE) int entityId,
                                                             @NotNull EntityType.EntityType0<T> type);

    <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@Range(from = 0L, to = Long.MAX_VALUE) int entityId);

    <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@Range(from = 0L, to = Long.MAX_VALUE) int entityId,
                                                                         @NotNull EntityType.LivingEntityType<T> type);

    static SurfBukkitPacketEntityApi get() {
        return SurfBukkitPacketApi.get().getEntityApi();
    }
}
