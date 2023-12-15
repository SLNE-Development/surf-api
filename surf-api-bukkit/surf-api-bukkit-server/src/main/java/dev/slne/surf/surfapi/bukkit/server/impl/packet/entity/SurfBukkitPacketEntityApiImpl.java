package dev.slne.surf.surfapi.bukkit.server.impl.packet.entity;

import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfLivingEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.interact.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType.EntityType0;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType.LivingEntityType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.entity.WrapperEntity;
import me.tofaa.entitylib.entity.WrapperLivingEntity;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;


/**
 * Implementation of the {@link SurfBukkitPacketEntityApi} interface.
 * Provides methods for registering and unregistering interact listeners,
 * creating entities, deleting entities, and retrieving entities by ID or UUID.
 */
@ApiStatus.Internal
public class SurfBukkitPacketEntityApiImpl implements SurfBukkitPacketEntityApi {
    private final List<SurfBukkitInteractListener> interactListeners;
    private final Object2ObjectMap<UUID, List<SurfBukkitInteractListener>> interactListenerMap;
    private final Object2ObjectMap<UUID, SurfEntity<?>> entities;
    private final Int2ObjectMap<SurfEntity<?>> entitiesById;

    public SurfBukkitPacketEntityApiImpl() {
        this.interactListeners = Collections.synchronizedList(new ArrayList<>());
        this.interactListenerMap = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        this.entities = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        this.entitiesById = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

        interactListenerMap.defaultReturnValue(new ArrayList<>()); // prevent NPEs

        registerInteractListener((clickedEntity, interactAction, interactionHand, user, player) -> {
            final List<SurfBukkitInteractListener> listeners = interactListenerMap.get(clickedEntity.getUuid());

            for (SurfBukkitInteractListener listener : listeners) {
                listener.onInteract(clickedEntity, interactAction, interactionHand, user, player);
            }
        });
    }


    @Override
    public void registerInteractListener(@NotNull SurfBukkitInteractListener listener) {
        interactListeners.add(listener);
    }

    @Override
    public void registerInteractListener(@NotNull UUID entity, @NotNull SurfBukkitInteractListener listener) {
        checkNotNull(entity, "entity");
        checkNotNull(listener, "listener");

        interactListenerMap.computeIfAbsent(entity, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public void registerInteractListener(@NotNull UUID entity,
                                         @NotNull Collection<SurfBukkitInteractListener> listener) {
        checkNotNull(entity, "entity");
        checkNotNull(listener, "listener");

        interactListenerMap.computeIfAbsent(entity, k -> new ArrayList<>()).addAll(listener);
    }

    @Override
    public void unregisterInteractListener(@NotNull SurfBukkitInteractListener listener) {
        interactListeners.remove(checkNotNull(listener, "listener"));
    }

    @Override
    public void unregisterInteractListener(@NotNull UUID entity) {
        interactListenerMap.remove(checkNotNull(entity, "entity"));
    }

    @Override
    public void unregisterInteractListener(@NotNull UUID entity, @NotNull SurfBukkitInteractListener listener) {
        interactListenerMap.get(checkNotNull(entity, "entity"))
                .remove(checkNotNull(listener, "listener"));
    }

    @Override
    public void unregisterInteractListener(@NotNull UUID entity,
                                           @NotNull Collection<SurfBukkitInteractListener> listener) {
        interactListenerMap.get(checkNotNull(entity, "entity"))
                .removeAll(checkNotNull(listener, "listener"));
    }

    @Override
    public @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid,
                                                                      @NotNull EntityType0<M> type) {
        return createEntity(uuid, type, meta -> {
        });
    }

    @Override
    public @NotNull <M extends LivingEntityMeta> SurfLivingEntity<M> createEntity(@NotNull UUID uuid,
                                                                                  @NotNull LivingEntityType<M> type) {
        return createEntity(uuid, type, meta -> {
        });
    }

    @Override
    public @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid,
                                                                      @NotNull EntityType0<M> type,
                                                                      @NotNull Consumer<M> changeMeta) {
        checkNotNull(uuid, "uuid");
        checkNotNull(type, "type");
        checkNotNull(changeMeta, "changeMeta");
        checkState(!entities.containsKey(uuid), "entity with UUID %s already exists", uuid);

        final WrapperEntity entity = EntityLib.createEntity(uuid, type.getType());
        final SurfEntity<M> surfEntity = new SurfEntity<>(checkNotNull(entity), type.getMetaClass());

        surfEntity.editMetadata(changeMeta);
        entities.put(uuid, surfEntity);
        checkState(
                !entitiesById.containsKey(entity.getEntityId()),
                "entity with ID %s already exists",
                entity.getEntityId()
        );
        entitiesById.put(entity.getEntityId(), surfEntity);

        return surfEntity;
    }

    @Override
    public @NotNull <M extends LivingEntityMeta> SurfLivingEntity<M> createEntity(@NotNull UUID uuid,
                                                                                  @NotNull LivingEntityType<M> type,
                                                                                  @NotNull Consumer<M> changeMeta) {
        checkNotNull(uuid, "uuid");
        checkNotNull(type, "type");
        checkNotNull(changeMeta, "changeMeta");
        checkState(!entities.containsKey(uuid), "entity with UUID %s already exists", uuid);

        final WrapperLivingEntity entity = ((WrapperLivingEntity) EntityLib.createEntity(uuid, type.getType()));
        final SurfLivingEntity<M> surfLivingEntity = new SurfLivingEntity<>(checkNotNull(entity), type.getMetaClass());

        surfLivingEntity.editMetadata(changeMeta);
        entities.put(uuid, surfLivingEntity);
        checkState(
                !entitiesById.containsKey(entity.getEntityId()),
                "entity with ID %s already exists",
                entity.getEntityId()
        );
        entitiesById.put(entity.getEntityId(), surfLivingEntity);

        return surfLivingEntity;
    }

    @Override
    public void deleteEntity(@NotNull UUID uuid) {
        checkNotNull(uuid, "uuid");

        final SurfEntity<?> surfEntity = entities.remove(uuid);
        entitiesById.remove(surfEntity.getEntityId());
        surfEntity.remove();

        unregisterInteractListener(uuid);
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid) {
        return getEntityByIdOrUUID(uuid, null, false);
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid, @NotNull EntityType0<T> type) {
        return getEntityByIdOrUUID(uuid, type, false);
    }

    @Override
    public <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@NotNull UUID uuid) {
        return getEntityByIdOrUUID(uuid, null, true);
    }

    @Override
    public <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(@NotNull UUID uuid,
                                                                                      @NotNull LivingEntityType<T> type) {
        return getEntityByIdOrUUID(uuid, type, true);
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(int entityId) {
        return getEntityByIdOrUUID(entityId, null, false);
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(int entityId, @NotNull EntityType0<T> type) {
        return getEntityByIdOrUUID(entityId, type, false);
    }

    @Override
    public <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(int entityId) {
        return getEntityByIdOrUUID(entityId, null, true);
    }

    @Override
    public <T extends LivingEntityMeta> Optional<SurfLivingEntity<T>> getLivingEntity(int entityId,
                                                                                      @NotNull LivingEntityType<T> type) {
        return getEntityByIdOrUUID(entityId, type, true);
    }

    /**
     * Retrieves an entity by either its ID or UUID.
     *
     * @param id       The ID or UUID of the entity
     * @param type     The entity type to filter the result (optional). Pass null to skip type checking.
     * @param isLiving Specifies if the entity should be a living entity (optional). Set to true to filter for living entities only.
     * @param <T>      The type of the SurfEntity
     * @param <M>      The type of the EntityMeta associated with the SurfEntity
     * @return An Optional containing the SurfEntity if found, or an empty Optional if not found or if the entity does not meet the specified filters
     */
    @ApiStatus.Internal
    private <T extends SurfEntity<M>, M extends EntityMeta> Optional<T> getEntityByIdOrUUID(Object id,
                                                                                            @Nullable EntityType0<M> type,
                                                                                            boolean isLiving) {
        checkArgument(id instanceof UUID || id instanceof Integer, "id must be UUID or int");

        if (id instanceof Integer integer) {
            checkArgument(integer >= 0, "entityId must be positive");
        }

        try {
            // Get the SurfEntity by UUID or entityId
            final SurfEntity<?> surfBaseEntity = (id instanceof UUID uuid) ? entities.get(uuid) : entitiesById.get(((int) id));

            if (type != null && !type.getMetaClass().isAssignableFrom(surfBaseEntity.getMetaClass())) { // Check if the entity is of the correct type (if specified)
                ComponentLogger.logger().error(
                        "The entity with {} {} is not of type {}",
                        (id instanceof UUID) ? "UUID" : "ID",
                        id,
                        type.getType().getName().getNamespace()
                );
                return Optional.empty();
            }

            if (isLiving && !(surfBaseEntity instanceof SurfLivingEntity<?>)) { // Check if the entity is a living entity (if specified)
                ComponentLogger.logger().error(
                        "The entity with {} {} is not a living entity! You may want to use getEntity instead.",
                        (id instanceof UUID) ? "UUID" : "ID",
                        id
                );
                return Optional.empty();
            }

            //noinspection unchecked - We already checked the type
            return Optional.of(((T) surfBaseEntity)); // Cast the SurfEntity to the correct type
        } catch (ClassCastException exception) {
            ComponentLogger.logger().warn(
                    "Failed to cast entity with {} {} to SurfEntity",
                    (id instanceof UUID) ? "UUID" : "ID",
                    id,
                    exception
            );
        }

        return Optional.empty(); // Something went wrong
    }

    /**
     * Retrieves the list of SurfBukkitInteractListeners.
     *
     * @return The list of SurfBukkitInteractListeners.
     */
    @ApiStatus.Internal
    public List<SurfBukkitInteractListener> getInteractListeners() {
        return interactListeners;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
