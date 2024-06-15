package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities;

import static com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ADV_COMPONENT;
import static com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ENTITY_POSE;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.google.common.base.MoreObjects;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.api.packet.entity.EntityStatus;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import dev.slne.surf.surfapi.core.api.util.LocationFactory;
import dev.slne.surf.surfapi.core.api.util.pos.RelativeLocation;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketEntityApiImpl;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PacketEntityImpl<T extends PacketEntity<T>> extends
    PacketEntityCommon implements PacketEntity<T>, SpawnVelocity, Shootable {

  protected static final int NO_DATA = 0;

  private final @NotNull UUID uuid;
  private final int entityId;

  protected @Nullable Location location;
  protected int shooterEntityId;
  protected boolean spawned;
  protected boolean batchUpdate;
  protected @NotNull Set<UUID> viewers;
  protected @NotNull Object2LongMap<UUID> onCooldown;
  protected @NotNull Object2LongMap<InteractAction> cooldownMillis;
  protected boolean overrideCooldown;
  protected long overrideCooldownMillis;
  protected @NotNull SurfInteractHandler<T> internalInteractHandler;
  private @Nullable Vector3d velocityAtSpawn;
  private @Nullable SurfInteractHandler<T> interactHandler;

  public PacketEntityImpl(@NotNull UUID uuid, EntityType type) {
    super(type);

    this.uuid = uuid;
    this.entityId = SurfCorePacketEntityApi.get().idProvider().nextEntityId();

    this.location = null;
    this.velocityAtSpawn = null;
    this.shooterEntityId = -1;
    this.spawned = false;
    this.batchUpdate = false;

    this.viewers = Collections.synchronizedSet(new HashSet<>());
    this.onCooldown = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());
    this.cooldownMillis = Object2LongMaps.synchronize(
        new Object2LongOpenHashMap<>(InteractAction.VALUES.length));
    this.overrideCooldown = false;
    this.overrideCooldownMillis = 0L;

    this.interactHandler = null;
    this.internalInteractHandler = createInternalInteractHandler();

    init();
  }

  private SurfInteractHandler<T> createInternalInteractHandler() {
    return (entity, interactAction, interactionHand, user) -> {
      if (interactHandler == null) {
        return;
      }

      final long currentTimeMillis = System.currentTimeMillis();
      final long cooldownMillis =
          overrideCooldown ? overrideCooldownMillis : this.cooldownMillis.getLong(interactAction);

      if (currentTimeMillis - onCooldown.getLong(user.getUUID()) < cooldownMillis) {
        return;
      }

      onCooldown.put(user.getUUID(), currentTimeMillis);
      interactHandler.handle(entity, interactAction, interactionHand, user);
    };
  }

  protected void init() {
    onCooldown.defaultReturnValue(0L);
    cooldownMillis.defaultReturnValue(0L);
    addSupportedVersion(LATEST_CLIENT_VERSION); // TODO: add support for other versions
  }

  @Override
  public final @NotNull UUID uuid() {
    return uuid;
  }

  @Override
  public final int entityId() {
    return entityId;
  }

  @Override
  public void animate(WrapperPlayServerEntityAnimation.EntityAnimationType animation) {
    checkDeleted();

    final WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(entityId,
        checkNotNull(animation, "Animation may not be null"));
    sendPacketToAllViewers(version -> packet);
  }

  @Override
  public void entityStatus(@NotNull EntityStatus status) {
    checkDeleted();

    final WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(entityId,
        checkNotNull(status, "Status may not be null").getStatusId());
    sendPacketToAllViewers(version -> packet);
  }

  @Override
  public final boolean onFire() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_ON_FIRE_BYTE);
  }

  @Override
  public final void onFire(boolean onFire) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_ON_FIRE_BYTE, onFire);
    afterSet();
  }

  @Override
  public final boolean sneaking() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_CROUCHING);
  }

  @Override
  public final void sneaking(boolean sneaking) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_CROUCHING, sneaking);
    afterSet();
  }

  @Override
  public final boolean sprinting() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_SPRINTING);
  }

  @Override
  public final void sprinting(boolean sprinting) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_SPRINTING, sprinting);
    afterSet();
  }

  @Override
  public final boolean invisible() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_INVISIBLE);
  }

  @Override
  public final void invisible(boolean invisible) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_INVISIBLE, invisible);
    afterSet();
  }

  @Override
  public final int airTicks() {
    return get(AIR_TICKS_INDEX, 300);
  }

  @Override
  public final void airTicks(int airTicks) {
    set(AIR_TICKS_INDEX, airTicks);
    afterSet();
  }

  @Override
  public final boolean glowingEffect() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, HAS_GLOWING_EFFECT);
  }

  @Override
  public final void glowingEffect(boolean glowingEffect) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, HAS_GLOWING_EFFECT, glowingEffect);
    afterSet();
  }

  @Override
  public final boolean swimming() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_SWIMMING);
  }

  @Override
  public final void swimming(boolean swimming) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_SWIMMING, swimming);
    afterSet();
  }

  @Override
  public final boolean flyingWithElytra() {
    return getMaskBit(ENTITY_BIT_MASK_INDEX, IS_FLYING_WITH_ELYTRA);
  }

  @Override
  public final void flyingWithElytra(boolean flyingWithElytra) {
    setMaskBit(ENTITY_BIT_MASK_INDEX, IS_FLYING_WITH_ELYTRA, flyingWithElytra);
    afterSet();
  }

  @Override
  public final boolean silent() {
    return get(IS_SILENT_INDEX, false);
  }

  @Override
  public final void silent(boolean silent) {
    set(IS_SILENT_INDEX, silent);
    afterSet();
  }

  @Override
  public final boolean noGravity() {
    return get(HAS_NO_GRAVITY_INDEX, false);
  }

  @Override
  public final void noGravity(boolean noGravity) {
    set(HAS_NO_GRAVITY_INDEX, noGravity);
    afterSet();
  }

  @Override
  public final int ticksFrozenInPowderedSnow() {
    return get(TICKS_FROZEN_IN_POWDERED_SNOW, 0);
  }

  @Override
  public final void ticksFrozenInPowderedSnow(int ticksFrozenInPowderedSnow) {
    set(TICKS_FROZEN_IN_POWDERED_SNOW, ticksFrozenInPowderedSnow);
    afterSet();
  }

  @Override
  public final @NotNull EntityPose entityPose() {
    return get(POSE_INDEX, EntityPose.STANDING);
  }

  @Override
  public final void entityPose(@NotNull EntityPose entityPose) {
    set(POSE_INDEX, ENTITY_POSE, entityPose);
    afterSet();
  }

  @Override
  public final Optional<Component> displayName() {
    return Optional.ofNullable(
        !get(CUSTOM_NAME_VISIBLE_INDEX, false) ? null : get(CUSTOM_NAME_INDEX, null));
  }

  @Override
  public void displayName(@Nullable Component displayName) {
    if (displayName == null) {
      set(CUSTOM_NAME_VISIBLE_INDEX, false);
    } else {
      set(CUSTOM_NAME_VISIBLE_INDEX, true);
      set(CUSTOM_NAME_INDEX, ADV_COMPONENT, displayName);
    }
    afterSet();
  }

  @Override
  public final @NotNull Vector3d velocityAtSpawn() {
    checkDeleted();
    return velocityAtSpawn == null ? Vector3d.zero() : velocityAtSpawn;
  }

  @Override
  public final void velocityAtSpawn(@NotNull Vector3d velocityAtSpawn) {
    checkDeleted();
    this.velocityAtSpawn = checkNotNull(velocityAtSpawn, "Velocity at spawn may not be null");
  }

  @Override
  public final int shooterEntityId() {
    return shooterEntityId;
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public void shooterEntityId(int shooterEntityId) {
    checkDeleted();
    this.shooterEntityId = shooterEntityId;
  }

  public int getData() { // Override this method to add data if not shooting entity
    return Math.max(shooterEntityId, NO_DATA);
  }

  @Override
  public final MiniMessage miniMessage() {
    return MiniMessage.miniMessage(); // TODO
  }

  @Override
  public final Optional<SurfInteractHandler<T>> interactHandler() {
    return Optional.of(internalInteractHandler);
  }

  @Override
  public final void interactHandler(@Nullable SurfInteractHandler<T> interactHandler) {
    checkDeleted();
    this.interactHandler = interactHandler;

    ((SurfCorePacketEntityApiImpl) SurfCorePacketEntityApi.get()).registerInteractListener();
  }

  @Override
  public void interactCooldown(long cooldown, @NotNull TimeUnit timeUnit, boolean soft) {
    checkDeleted();
    final long millis = timeUnit.toMillis(cooldown);
    this.cooldownMillis.defaultReturnValue(millis);

    if (!soft) {
      this.overrideCooldown = true;
      this.overrideCooldownMillis = millis;
      this.cooldownMillis.clear();
    } else {
      this.overrideCooldown = false;
    }
  }

  @Override
  public void interactCooldown(InteractAction action, long cooldown, @NotNull TimeUnit timeUnit) {
    checkDeleted();
    if (!overrideCooldown) {
      this.cooldownMillis.put(action, timeUnit.toMillis(cooldown));
    } else {
      ComponentLogger.logger()
          .warn("Interact cooldown is overridden, cooldown for action {} will be ignored " +
              "and not set", action);
    }
  }

  @Override
  public void resetInteractCooldown() {
    checkDeleted();
    this.overrideCooldown = false;
    this.overrideCooldownMillis = 0L;
  }

  @Override
  public void resetInteractCooldown(InteractAction action) {
    checkDeleted();
    this.cooldownMillis.removeLong(action);
  }

  @Override
  public boolean spawn(@NotNull Location location) {
    checkDeleted();
    checkNotNull(location, "Cannot spawn entity at null location");

    if (isSpawned()) {
      return false;
    }

    this.location = location;
    viewers.forEach(this::spawn);
    spawned = true;

    return true;
  }

  @Override
  public boolean respawn(@NotNull Location location) {
    checkDeleted();
    return despawn() && spawn(location);
  }

  @Override
  public boolean respawn() {
    checkDeleted();
    if (!isSpawned()) {
      return false;
    }

    return respawn(checkNotNull(location, "Cannot respawn entity with null location"));
  }

  @Override
  public final boolean isSpawned() {
    checkDeleted();
    return spawned;
  }

  @Override
  public final boolean despawn() {
    checkDeleted();
    if (!isSpawned()) {
      return false;
    }

    viewers.forEach(this::despawn);

    spawned = false;
    this.location = null;

    return true;
  }

  @Override
  public final boolean teleport(@NotNull Location location) {
    checkDeleted();
    checkNotNull(location, "Cannot teleport to null location");

    if (!isSpawned()) {
      return false;
    }

    assert this.location != null : "Cannot null when spawned is true";

    // check if the distance is bigger than 8 blocks
    if (LocationFactory.distanceSquared(location, this.location) > 64) {
      var packet = new WrapperPlayServerEntityTeleport(entityId, location, true);
      sendPacketToAllViewers(version -> packet);
    } else {
      var packet = RelativeLocation.of(location, this.location).toPacket(entityId, true);
      sendPacketToAllViewers(version -> packet);
    }

    this.location = location;

    return true;
  }

  @Override
  public boolean addViewer(@NotNull UUID uuid) {
    checkDeleted();
    final boolean success = viewers.add(checkNotNull(uuid, "Cannot add viewer with null uuid"));
    if (success && isSpawned()) {
      spawn(uuid);
    }

    return success;
  }

  @Override
  public boolean removeViewer(@NotNull UUID uuid) {
    checkDeleted();
    final boolean success = viewers.remove(
        checkNotNull(checkNotNull(uuid, "Cannot remove viewer with null uuid")));

    if (success) {
      despawn(uuid);
    }

    return success;
  }

  protected void spawn(UUID uuid) {
    checkDeleted();

    sendPacketsToViewer(uuid,
        __ -> new WrapperPlayServerBundle(),
        version1 -> spawnPacket(),
        this::metadataPacket,
        __ -> new WrapperPlayServerBundle());
  }

  protected void despawn(UUID uuid) {
    checkDeleted();
    sendPacketToViewer(uuid, this::despawnPacket);
  }

  @Override
  @SuppressWarnings("DataFlowIssue")
  // We are deleting the entity, so we don't care about the warnings
  public void delete() {
    if (isSpawned()) {
      despawn();
    }

    viewers = null;
    onCooldown = null;
    cooldownMillis = null;
    interactHandler = null;
    internalInteractHandler = null;
    location = null;
    velocityAtSpawn = null;

    super.delete();
  }

  protected PacketWrapper<?> spawnPacket() {
    assert location != null : "Cannot spawn entity with null location";
    return new WrapperPlayServerSpawnEntity(
        entityId,
        Optional.of(uuid),
        type,
        location.getPosition(),
        location.getPitch(),
        location.getYaw(),
        1,
        getData(),
        Optional.ofNullable(velocityAtSpawn)
    );
  }

  protected PacketWrapper<?> metadataPacket(ClientVersion version) {
    return new WrapperPlayServerEntityMetadata(entityId, this.entityData(version));
  }

  protected PacketWrapper<?> despawnPacket(ClientVersion version) {
    return new WrapperPlayServerDestroyEntities(entityId);
  }


  public void startBatchUpdate() {
    this.batchUpdate = true;
  }

  public void pushBatchUpdate() {
    this.batchUpdate = false;
    sendPacketToAllViewers(this::metadataPacket);
  }

  protected void afterSet() {
    if (!batchUpdate) {
      sendPacketToAllViewers(this::metadataPacket);
    }
  }

  protected void sendPacketToAllViewers(Function<ClientVersion, PacketWrapper<?>> packet) {
    final PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
    final SurfCoreApi surfCoreApi = SurfCoreApi.getCore();

    for (final UUID viewer : viewers) {
      surfCoreApi.getPlayer(viewer).ifPresent(player -> {
        final User user = playerManager.getUser(player);
        user.sendPacket(packet.apply(user.getClientVersion()));
      });
    }
  }

  protected void sendPacketToViewer(UUID viewer, Function<ClientVersion, PacketWrapper<?>> packet) {
    final PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
    final SurfCoreApi surfCoreApi = SurfCoreApi.getCore();

    surfCoreApi.getPlayer(viewer).ifPresent(player -> {
      final User user = playerManager.getUser(player);
      user.sendPacket(packet.apply(user.getClientVersion()));
    });
  }

  @SafeVarargs
  protected final void sendPacketsToViewer(UUID viewer,
      Function<ClientVersion, PacketWrapper<?>>... packets) {
    final PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
    final SurfCoreApi surfCoreApi = SurfCoreApi.getCore();

    surfCoreApi.getPlayer(viewer).ifPresent(player -> {
      final User user = playerManager.getUser(player);
      final ClientVersion clientVersion = user.getClientVersion();
      for (Function<ClientVersion, PacketWrapper<?>> packet : packets) {
        user.sendPacket(packet.apply(clientVersion));
      }
    });
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", uuid)
        .add("entityId", entityId)
        .add("spawned", spawned)
        .add("velocityAtSpawn", velocityAtSpawn)
        .add("shooterEntityId", shooterEntityId)
        .add("interactHandler", interactHandler)
        .add("viewers", viewers)
        .add("PacketEntityCommon", super.toString())
        .toString();
  }
}
