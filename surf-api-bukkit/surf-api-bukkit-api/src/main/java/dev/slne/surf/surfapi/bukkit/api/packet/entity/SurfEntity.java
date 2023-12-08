package dev.slne.surf.surfapi.bukkit.api.packet.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import me.tofaa.entitylib.entity.WrapperEntity;
import me.tofaa.entitylib.meta.EntityMeta;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a surf entity with metadata that can be modified and interacted with.
 *
 * @param <T>       the type of the entity's metadata
 * @param entity    the wrapped entity
 * @param metaClass the class of the entity's metadata
 */
@SuppressWarnings("unused")
@ApiStatus.NonExtendable
@ApiStatus.Internal
public record SurfEntity<T extends EntityMeta>(WrapperEntity entity, Class<T> metaClass) {

    /**
     * Edits the metadata of the entity and sends the updated metadata to all viewers.
     *
     * @param consumer the function to modify the entity's metadata
     */
    @SuppressWarnings("unchecked")
    public void editMetadata(Consumer<T> consumer) {
        consumer.accept((T) entity.getMeta());
        entity.sendPacketToViewers(entity.getMeta().createPacket());
    }

    /**
     * Sets a SurfBukkitInteractListener to listen for interact events on the SurfEntity.
     *
     * @param listener the SurfBukkitInteractListener to register for interact events
     */
    public void setOnInteract(SurfBukkitInteractListener listener) {
        SurfBukkitPacketApi.get().registerInteractListener(entity.getUuid(), listener);
    }

    /**
     * Spawns the entity at the specified location.
     *
     * @param location the location where the entity should be spawned
     * @return true if the entity was successfully spawned, false otherwise
     */
    public boolean spawn(Location location) {
        return entity.spawn(new com.github.retrooper.packetevents.protocol.world.Location(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch()));
    }

    /**
     * Rotates the head of the entity.
     *
     * @param yaw   the yaw angle in degrees
     * @param pitch the pitch angle in degrees
     */
    public void rotateHead(float yaw, float pitch) {
        entity.rotateHead(yaw, pitch);
    }

    /**
     * Removes the entity from the world.
     * This method removes the entity from the world and all viewers will no longer see it.
     * After invoking this method, the entity will no longer exist. But you can spawn it again.
     */
    public void remove() {
        entity.remove();
    }

    /**
     * Teleports the entity to the specified location.
     *
     * @param location The destination location to teleport the entity to.
     * @param onGround Whether the entity should be teleported on the ground or in the air.
     */
    public void teleport(Location location, boolean onGround) {
        entity.teleport(new com.github.retrooper.packetevents.protocol.world.Location(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch()), onGround);
    }

    /**
     * Teleports the entity to the specified location.
     *
     * @param location the destination location
     */
    public void teleport(Location location) {
        entity.teleport(new com.github.retrooper.packetevents.protocol.world.Location(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch()));
    }

    /**
     * Adds a viewer to the SurfEntity, allowing the viewer to see and interact
     * with the entity.
     *
     * @param uuid the UUID of the viewer to add
     * @return true if the viewer was added successfully, false otherwise
     */
    public boolean addViewer(UUID uuid) {
        return entity.addViewer(uuid);
    }

    /**
     * Adds a viewer to the entity.
     *
     * @param player the player viewer to be added
     * @return true if the viewer was successfully added, false otherwise
     */
    public boolean addViewer(@NotNull Player player) {
        return entity.addViewer(player.getUniqueId());
    }

    /**
     * Removes a viewer from the SurfEntity.
     * The viewer will no longer be able to see the entity.
     *
     * @param uuid the UUID of the viewer to be removed
     */
    public void removeViewer(UUID uuid) {
        entity.removeViewer(uuid);
    }

    /**
     * Removes a viewer from the entity by their UUID.
     *
     * @param player the player whose viewer will be removed
     */
    public void removeViewer(@NotNull Player player) {
        entity.removeViewer(player.getUniqueId());
    }

    /**
     * Retrieves the metadata of the entity.
     *
     * @return the metadata of the entity
     */
    @SuppressWarnings("unchecked")
    public T getMeta() {
        return (T) entity.getMeta();
    }

    /**
     * Returns the universally unique identifier (UUID) of the SurfEntity.
     *
     * @return the UUID of the SurfEntity
     */
    public UUID getUuid() {
        return entity.getUuid();
    }

    /**
     * Retrieves the EntityType of the SurfEntity.
     *
     * @return the EntityType of the SurfEntity
     */
    public EntityType getEntityType() {
        return entity.getEntityType();
    }

    /**
     * Returns the ID of the entity.
     *
     * @return the ID of the entity
     */
    public int getEntityId() {
        return entity.getEntityId();
    }

    /**
     * Check if the entity has been spawned.
     *
     * @return true if the entity has been spawned, false otherwise.
     */
    public boolean isSpawned() {
        return entity.hasSpawned();
    }
}
