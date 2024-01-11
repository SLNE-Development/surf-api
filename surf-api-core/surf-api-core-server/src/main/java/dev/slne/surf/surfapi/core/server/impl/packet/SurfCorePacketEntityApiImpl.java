package dev.slne.surf.surfapi.core.server.impl.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.other.PacketExperienceOrb;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import dev.slne.surf.surfapi.core.api.packet.events.GameEvent;
import dev.slne.surf.surfapi.core.api.packet.events.WorldEvent;
import dev.slne.surf.surfapi.core.api.packet.packets.WrapperPlayServerWorldEvent;
import dev.slne.surf.surfapi.core.api.util.ParticleFactory;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.SpawnablePacketEntityRegistry;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.other.PacketExperienceOrbImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import java.util.*;
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

                System.err.println("Interact: " + packet.getEntityId());

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

        registerEntity(entity);

        if (initializer != null) {
            entity.startBatchUpdate();
            initializer.accept(entity);
            entity.pushBatchUpdate();
        }

        return entity;
    }

    @Override
    public <T extends PacketEntity<T>> Optional<T> getEntity(@NotNull UUID uuid) {
        return Optional.ofNullable((T) entitiesByUuid.get(uuid));
    }

    @Override
    public <T extends PacketEntity<T>> Optional<T> getEntity(@NotNull UUID uuid, Class<T> entityClass) {
        final PacketEntity<?> entity = entitiesByUuid.get(uuid);

        if (entity == null) {
            return Optional.empty();
        }

        checkState(entityClass.isInstance(entity), "Entity with UUID '%s' is not an instance of '%s' (it's '%s')", uuid, entityClass, entity.getClass());

        return Optional.ofNullable((T) entity);
    }

    @Override
    public <T extends PacketEntity<T>> Optional<T> getEntity(@Range(from = 0L, to = Integer.MAX_VALUE) int entityId) {
        return Optional.ofNullable((T) entitiesById.get(entityId));
    }

    @Override
    public <T extends PacketEntity<T>> Optional<T> getEntity(@Range(from = 0L, to = Integer.MAX_VALUE) int entityId, Class<T> entityClass) {
        final PacketEntity<?> entity = entitiesById.get(entityId);

        if (entity == null) {
            return Optional.empty();
        }

        checkState(entityClass.isInstance(entity), "Entity with entityId '%s' is not an instance of '%s' (it's '%s')", entityId, entityClass, entity.getClass());

        return Optional.ofNullable((T) entity);
    }

    public <T extends PacketEntity<T>> void deleteEntity(@NotNull T entity) {
        checkNotNull(entity, "Entity may not be null");

        entitiesById.remove(entity.entityId());
        entitiesByUuid.remove(entity.uuid());
        ((PacketEntityImpl<T>) entity).delete();
    }

    @Override
    public PacketExperienceOrb spawnExperienceOrb(@NotNull Vector3i position, short experience, UUID @NotNull ... viewers) {
        checkNotNull(position, "Position may not be null");
        checkNotNull(viewers, "Viewers may not be null");

        final PacketExperienceOrb orb = new PacketExperienceOrbImpl(position, experience);

        for (UUID viewer : viewers) {
            orb.addViewer(viewer);
        }

        return orb;
    }

    @Override
    public void spawnExplosion(@NotNull Vector3d position,
                               @Range(from = 0, to = (long) Float.MAX_VALUE) float power,
                               @Nullable List<Vector3i> relativeAffectedBlocks,
                               @Nullable Vector3f playerVelocity,
                               @Nullable Particle smallExplosionParticle,
                               @Nullable Particle largeExplosionParticle,
                               WrapperPlayServerExplosion.@Nullable BlockInteraction blockAction,
                               @Nullable Key explosionSound,
                               @Nullable Float explosionSoundRange,
                               @ArrayLenRange(from = 1) UUID @NotNull ... viewers) {
        checkNotNull(position, "Position may not be null");
        checkNotNull(viewers, "Viewers may not be null");
        checkArgument(explosionSoundRange == null || explosionSoundRange >= 0, "Explosion sound range may not be less than 0");
        checkArgument(power >= 0, "Power may not be less than 0");

        final WrapperPlayServerExplosion packet = new WrapperPlayServerExplosion(
                new com.github.retrooper.packetevents.util.Vector3d(position.x(), position.y(), position.z()),
                power,
                relativeAffectedBlocks == null ? new ArrayList<>() : relativeAffectedBlocks.stream().map(vector3i -> new com.github.retrooper.packetevents.util.Vector3i(vector3i.x(), vector3i.y(), vector3i.z())).toList(),
                playerVelocity == null ? com.github.retrooper.packetevents.util.Vector3f.zero() : new com.github.retrooper.packetevents.util.Vector3f(playerVelocity.x(), playerVelocity.y(), playerVelocity.z()),
                smallExplosionParticle == null ? ParticleFactory.of(ParticleTypes.EXPLOSION) : smallExplosionParticle,
                largeExplosionParticle == null ? ParticleFactory.of(ParticleTypes.EXPLOSION_EMITTER) : largeExplosionParticle,
                blockAction == null ? WrapperPlayServerExplosion.BlockInteraction.DESTROY_BLOCKS : blockAction,
                explosionSound == null ? ResourceLocation.minecraft("entity.generic.explode") : new ResourceLocation(explosionSound.namespace(), explosionSound.value()),
                explosionSoundRange == null ? 0 : explosionSoundRange
        );
        final SurfCorePacketApi packetApi = SurfCorePacketApi.get();

        for (UUID viewer : viewers) {
            packetApi.sendPacket(viewer, packet);
        }
    }

    @Override
    public <T> void gameEvent(GameEvent.Type<T> type, @CheckForNull T value, UUID @NotNull ... viewers) {
        checkNotNull(type, "Type may not be null");
        checkNotNull(viewers, "Viewers may not be null");

        final WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(
                type.getId(),
                value == null ? 0 : type.encode(value)
        );

        final SurfCorePacketApi packetApi = SurfCorePacketApi.get();
        for (UUID viewer : viewers) {
            packetApi.sendPacket(viewer, packet);
        }
    }

    @Override
    public void playHurtAnimation(int entityId, float yaw, UUID @NotNull ... viewers) {
        checkNotNull(viewers, "Viewers may not be null");

        final WrapperPlayServerHurtAnimation packet = new WrapperPlayServerHurtAnimation(entityId, yaw);

        final SurfCorePacketApi packetApi = SurfCorePacketApi.get();
        for (UUID viewer : viewers) {
            packetApi.sendPacket(viewer, packet);
        }
    }

    @Override
    public <T> void worldEvent(WorldEvent.@NotNull Type<T> type, Vector3i position, @Nullable T value, UUID @NotNull ... viewers) {
        sendWorldEvent(type.getId(), position, value == null ? 0 : type.encode(value), false, viewers);
    }

    @Override
    public <T> void worldEvent(WorldEvent.@NotNull AllowBooleanType<T> type, Vector3i position, @Nullable T value, boolean global, UUID @NotNull ... viewers) {
        sendWorldEvent(type.getId(), position, value == null ? 0 : type.encode(value), global, viewers);
    }

    private void sendWorldEvent(int id, @NotNull Vector3i position, int data, boolean global, UUID @NotNull ... viewers) {
        final WrapperPlayServerWorldEvent packet = new WrapperPlayServerWorldEvent(
                id,
                new com.github.retrooper.packetevents.util.Vector3i(position.x(), position.y(), position.z()),
                data,
                global
        );

        final SurfCorePacketApi packetApi = SurfCorePacketApi.get();
        for (UUID viewer : viewers) {
            packetApi.sendPacket(viewer, packet);
        }
    }

    private <T extends PacketEntity<T>> void registerEntity(T entity) {
        entitiesById.put(entity.entityId(), entity);
        entitiesByUuid.put(entity.uuid(), entity);
    }
}
