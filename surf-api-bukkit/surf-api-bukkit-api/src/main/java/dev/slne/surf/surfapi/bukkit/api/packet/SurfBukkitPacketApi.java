package dev.slne.surf.surfapi.bukkit.api.packet;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfLivingEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.interact.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandlerSimple;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;


/**
 * SurfBukkitPacketApi interface extends SurfCorePacketApi interface
 * and provides additional methods for packet handling in Bukkit.
 * It allows registering and unregistering packet lore listeners, interacting with entities, and creating entities.
 */
@ApiStatus.NonExtendable
public interface SurfBukkitPacketApi extends SurfCorePacketApi {

    /**
     * Retrieves the SurfBukkitPacketEntityApi instance.
     *
     * @return surfBukkitPacketEntityApi instance
     */
    SurfBukkitPacketEntityApi getEntityApi();

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
    default void registerPacketLoreListener(@NotNull NamespacedKey identifier,
                                            @NotNull SurfBukkitPacketLoreHandlerSimple listener) {
        registerPacketLoreListener(identifier, (SurfBukkitPacketLoreHandler) listener);
    }

    /**
     * Unregisters a packet lore listener identified by the given identifier.
     *
     * @param identifier the identifier of the packet lore listener to unregister
     */
    void unregisterPacketLoreListener(@NotNull NamespacedKey identifier);

    /**
     * Registers a SurfBukkitInteractListener to listen for interact events.
     *
     * @param listener the SurfBukkitInteractListener to register
     */
    default void registerPacketEntityInteractListener(@NotNull SurfBukkitInteractListener listener) {
        getEntityApi().registerInteractListener(listener);
    }

    /**
     * Registers a SurfBukkitInteractListener to listen for interact events on a specific entity.
     *
     * @param entity   the UUID of the entity
     * @param listener the SurfBukkitInteractListener to register
     */
    default void registerPacketEntityInteractListener(@NotNull UUID entity,
                                                      @NotNull SurfBukkitInteractListener listener){
        getEntityApi().registerInteractListener(entity, listener);
    }

    /**
     * Unregisters a SurfBukkitInteractListener.
     *
     * @param listener the SurfBukkitInteractListener to unregister
     */
    default void unregisterPacketEntityInteractListener(@NotNull SurfBukkitInteractListener listener){
        getEntityApi().unregisterInteractListener(listener);
    }

    /**
     * Unregisters a SurfBukkitInteractListener from a specific entity.
     *
     * @param entity the UUID of the entity
     */
    default void unregisterPacketEntityInteractListener(@NotNull UUID entity){
        getEntityApi().unregisterInteractListener(entity);
    }

    /**
     * Creates a SurfEntity with the given UUID, type, and metadata modifier.
     *
     * @param uuid       the UUID of the entity (must be unique)
     * @param type       the type of the entity
     * @param changeMeta a Consumer function to modify the metadata of the entity
     * @param <M>        the type of the entity's metadata
     * @return a SurfEntity object representing the created entity
     */
    @NotNull
    default <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid,
                                                              @NotNull EntityType.EntityType0<M> type,
                                                              @NotNull Consumer<M> changeMeta) {
        return getEntityApi().createEntity(uuid, type, changeMeta);
    }

    /**
     * Creates a living entity with the given UUID and type.
     *
     * @param uuid the UUID of the entity (must be unique)
     * @param type the type of the entity
     * @param <M>  the type of the entity's metadata
     * @return a SurfLivingEntity object representing the created entity
     */
    @NotNull
    default <M extends LivingEntityMeta> SurfLivingEntity<M> createEntity(@NotNull UUID uuid,
                                                                          @NotNull EntityType.LivingEntityType<M> type,
                                                                          @NotNull Consumer<M> changeMeta) {
        return getEntityApi().createEntity(uuid, type, changeMeta);
    }

    /**
     * Retrieves the SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    static SurfBukkitPacketApi get() {
        return SurfBukkitApi.get().getPacketApi();
    }
}
