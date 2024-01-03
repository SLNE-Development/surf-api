package dev.slne.surf.surfapi.core.server.impl.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.SpawnablePacketEntityRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.*;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public abstract class SurfCorePacketEntityApiImpl implements SurfCorePacketEntityApi {

    private boolean interactListenerRegistered = false;
    private @NotNull EntityIdProvider idProvider;
    private final SpawnablePacketEntityRegistry registry;
    private final Set<SurfInteractHandler<? extends PacketEntity<?>>> interactHandlers;
    private final Int2ObjectMap<PacketEntity<?>> entitiesById;
    private final Object2ObjectMap<UUID, PacketEntity<?>> entitiesByUuid;

    protected SurfCorePacketEntityApiImpl(@NotNull EntityIdProvider defaultIdProvider) {
        this.idProvider = defaultIdProvider;
        this.registry = new SpawnablePacketEntityRegistry();
        this.interactHandlers = Collections.synchronizedSet(new HashSet<>());
        this.entitiesByUuid = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        this.entitiesById = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    }

    public void registerInteractListener() {
        if (interactListenerRegistered) {
            return;
        }

        PacketEvents.getAPI().getEventManager().registerListener(new SimplePacketListenerAbstract() {
            @Override
            public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
                    return;
                }

                final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
                final PacketEntity<?> packetEntity = entitiesById.get(packet.getEntityId());
                final WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
                final InteractionHand hand = packet.getHand();
                final User user = event.getUser();

                if (packetEntity == null) {
                    return;
                }

                packetEntity.interactHandler().ifPresent(handler -> handler.handleInternal(packetEntity, action, hand, user));

                for (SurfInteractHandler<? extends PacketEntity<?>> handler : interactHandlers) {
                    handler.handleInternal(packetEntity, action, hand, user);
                }
            }
        });
        interactListenerRegistered = true;
    }

    @Override
    public void idProvider(@NotNull EntityIdProvider provider) {
        this.idProvider = checkNotNull(provider, "Id provider may not be null");
    }

    @Override
    public @NotNull EntityIdProvider idProvider() {
        return idProvider;
    }

    @Override
    public <T extends PacketEntity<T>> void registerInteractHandler(SurfInteractHandler<T> handler) {
        interactHandlers.add(checkNotNull(handler, "Handler may not be null"));
        registerInteractListener();
    }

    @Override
    public <T extends PacketEntity<T> & Spawnable> T spawnEntity(@NotNull Class<T> entityClass, @NotNull UUID uuid, @Nullable Consumer<T> initializer) {
        checkState(!entitiesByUuid.containsKey(uuid), "Entity with UUID %s already exists", uuid);

        final Function<UUID, T> constructor = registry.get(entityClass);
        final T entity = constructor.apply(checkNotNull(uuid, "UUID may not be null"));

        if (initializer != null) {
            entity.startBatchUpdate();
            initializer.accept(entity);
            entity.pushBatchUpdate();
        }

        return entity;
    }
}
