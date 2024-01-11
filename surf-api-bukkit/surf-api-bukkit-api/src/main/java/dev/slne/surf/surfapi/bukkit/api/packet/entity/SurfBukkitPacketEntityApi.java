package dev.slne.surf.surfapi.bukkit.api.packet.entity;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfBukkitPacketEntityApi extends SurfCorePacketEntityApi {

//
//    void deleteEntity(@NotNull UUID uuid);
//
//    /**
//     * Retrieves the SurfEntity with the specified UUID.
//     *
//     * @param uuid The UUID of the entity to retrieve.
//     * @param <T>  The type of the entity's metadata.
//     * @return An Optional containing the SurfEntity if an entity with the specified UUID exists, otherwise empty.
//     */
//    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid);
//
//    /**
//     * Retrieves the SurfEntity with the specified entityId.
//     *
//     * @param entityId the ID of the entity
//     * @param <T>      the type of the entity's metadata
//     * @return an Optional representing the SurfEntity with the specified entityId, or an empty Optional if no entity is found
//     */
//    <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@Range(from = 0L, to = Long.MAX_VALUE) int entityId);
    static SurfBukkitPacketEntityApi get() {
        return SurfBukkitPacketApi.get().getPacketEntityApi();
    }
}
