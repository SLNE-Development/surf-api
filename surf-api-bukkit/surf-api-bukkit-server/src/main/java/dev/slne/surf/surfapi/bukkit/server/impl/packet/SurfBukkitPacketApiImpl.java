package dev.slne.surf.surfapi.bukkit.server.impl.packet;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.bukkit.server.packet.lore.PacketLoreListener;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketApiImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.entity.WrapperEntity;
import me.tofaa.entitylib.meta.EntityMeta;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

public class SurfBukkitPacketApiImpl extends SurfCorePacketApiImpl implements SurfBukkitPacketApi {
    /**
     * List of SurfBukkitInteractListener objects.
     * The list is synchronized to support concurrent access.
     * Use the registerInteractListener method to add an interact listener to the list.
     * Use the getInteractListeners method to retrieve the list of interact listeners.
     */
    private final List<SurfBukkitInteractListener> interactListeners = Collections.synchronizedList(new ArrayList<>());
    private final Object2ObjectMap<UUID, SurfBukkitInteractListener> interactListenerMap = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private final Object2ObjectMap<UUID, SurfEntity<?>> entities = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private final Int2ObjectMap<SurfEntity<?>> entitiesById = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public SurfBukkitPacketApiImpl() {
        registerInteractListener((clickedEntity, interactAction, interactionHand, user, player) -> {
            final SurfBukkitInteractListener listener = interactListenerMap.get(clickedEntity.getUuid());
            if (listener != null) {
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

        interactListenerMap.put(entity, listener);
    }

    @Override
    public void unregisterInteractListener(@NotNull SurfBukkitInteractListener listener) {
        interactListeners.remove(listener);
    }

    @Override
    public void unregisterInteractListener(@NotNull UUID entity) {
        interactListenerMap.remove(entity);
    }

    @Override
    public @NotNull <M extends EntityMeta, T extends EntityType<M>> SurfEntity<M> createEntity(@NotNull UUID uuid, @NotNull T type) {
        return createEntity(uuid, type, meta -> {
        });
    }

    public @NotNull <M extends EntityMeta> SurfEntity<M> createEntity(@NotNull UUID uuid, @NotNull EntityType<M> type, @NotNull Consumer<M> changeMeta) {
        checkNotNull(uuid, "uuid");
        checkNotNull(type, "type");
        checkNotNull(changeMeta, "changeMeta");
        checkState(!entities.containsKey(uuid), "entity with UUID %s already exists", uuid);

        final WrapperEntity entity = EntityLib.createEntity(uuid, type.getType());
        final SurfEntity<M> surfEntity = new SurfEntity<>(checkNotNull(entity), type.getMetaClass());

        surfEntity.editMetadata(changeMeta);
        entities.put(uuid, surfEntity);
        entitiesById.put(entity.getEntityId(), surfEntity);

        return surfEntity;
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(@NotNull UUID uuid) {
        checkNotNull(uuid, "uuid");

        try {
            return Optional.ofNullable((SurfEntity<T>) entities.get(uuid));
        } catch (ClassCastException e) {
            ComponentLogger.logger().warn("Failed to cast entity with UUID {} to SurfEntity", uuid, e);
            return Optional.empty();
        }
    }

    @Override
    public <T extends EntityMeta> Optional<SurfEntity<T>> getEntity(int entityId) {
        try {
            return Optional.ofNullable((SurfEntity<T>) entitiesById.get(entityId));
        } catch (ClassCastException e) {
            ComponentLogger.logger().warn("Failed to cast entity with ID {} to SurfEntity", entityId, e);
            return Optional.empty();
        }
    }

    @Override
    public void registerPacketLoreListener(@NotNull NamespacedKey identifier, @NotNull SurfBukkitPacketLoreHandler listener) {
        checkNotNull(identifier, "identifier");
        checkNotNull(listener, "listener");

        PacketLoreListener.INSTANCE.register(identifier, listener);
    }

    @Override
    public void unregisterPacketLoreListener(@NotNull NamespacedKey identifier) {
        checkNotNull(identifier, "identifier");

        PacketLoreListener.INSTANCE.unregister(identifier);
    }

    /**
     * Returns the list of SurfBukkitInteractListener objects.
     *
     * @return the list of SurfBukkitInteractListener objects
     */
    @ApiStatus.Internal
    public List<SurfBukkitInteractListener> getInteractListeners() {
        return interactListeners;
    }
}
