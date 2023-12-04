package dev.slne.surf.surfapi.bukkit.api.packet;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import me.tofaa.entitylib.meta.EntityMeta;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface SurfBukkitPacketApi extends SurfCorePacketApi {

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

    /**
     * Unregisters a SurfBukkitInteractListener.
     *
     * @param listener the SurfBukkitInteractListener to unregister
     */
    void unregisterInteractListener(@NotNull SurfBukkitInteractListener listener);

    /**
     * Unregisters a SurfBukkitInteractListener from a specific entity.
     *
     * @param entity   the UUID of the entity
     */
    void unregisterInteractListener(@NotNull UUID entity);

    /**
     * Creates an entity with the given UUID and type.
     *
     * @param uuid the UUID of the entity (must be unique)
     * @param type the type of the entity
     * @param <M>  the type of the entity's metadata
     * @param <T>  the type of the entity
     * @return a SurfEntity object representing the created entity
     */
    @NotNull <M extends EntityMeta, T extends EntityType<M>> SurfEntity<M> createEntity(@NotNull UUID uuid, @NotNull T type);

    /**
     * Creates a SurfEntity with the given UUID, type, and metadata modifier.
     *
     * @param uuid       the UUID of the entity (must be unique)
     * @param type       the type of the entity
     * @param changeMeta a Consumer function to modify the metadata of the entity
     * @param <M>        the type of the entity's metadata
     * @return a SurfEntity object representing the created entity
     */
    @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid, @NotNull EntityType<M> type, @NotNull Consumer<M> changeMeta);

    /**
     * Retrieves the SurfEntity with the specified UUID.
     *
     * @param uuid The UUID of the entity to retrieve.
     * @param <T> The type of the entity's metadata.
     * @return An Optional containing the SurfEntity if an entity with the specified UUID exists, otherwise empty.
     */
    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid);

    /**
     * Retrieves the SurfEntity with the specified entityId.
     *
     * @param entityId the ID of the entity
     * @param <T>      the type of the entity's metadata
     * @return an Optional representing the SurfEntity with the specified entityId, or an empty Optional if no entity is found
     */
    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(int entityId);

    /**
     * Registers a listener for modifying the lore of an item stack.
     *
     * @param identifier     The identifier for the item to listen for. Must not be null.
     * @param listener       The listener for modifying the lore of an item stack.
     */
    void registerPacketLoreListener(@NotNull NamespacedKey identifier, @NotNull SurfBukkitPacketLoreHandler listener);

    /**
     * Registers a packet lore listener with the specified identifier.
     *
     * @param identifier The identifier for the item to listen for. Must not be null.
     * @param listener   The packet lore listener to register. Must not be null.
     */
    default void registerPacketLoreListener(@NotNull NamespacedKey identifier, @NotNull SurfBukkitPacketLoreHandler.SurfBukkitPacketLoreHandlerSimple listener) {
        registerPacketLoreListener(identifier, (SurfBukkitPacketLoreHandler) listener);
    }

    /**
     * Unregisters a packet lore listener identified by the given identifier.
     *
     * @param identifier the identifier of the packet lore listener to unregister
     */
    void unregisterPacketLoreListener(@NotNull NamespacedKey identifier);

    /**
     * Retrieves the SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    static SurfBukkitPacketApi get() {
        return SurfBukkitApi.get().getPacketApi();
    }
}
