package dev.slne.surf.surfapi.core.api.packet.entity.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityMetadataProvider;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.world.Location;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@ApiStatus.NonExtendable
public interface PacketEntity<Impl extends PacketEntity<Impl>> extends EntityMetadataProvider {

    /**
     * Index numbers for packets
     */
    int ENTITY_BIT_MASK_INDEX = 0, AIR_TICKS_INDEX = 1, CUSTOM_NAME_INDEX = 2, CUSTOM_NAME_VISIBLE_INDEX = 3,
            IS_SILENT_INDEX = 4, HAS_NO_GRAVITY_INDEX = 5, POSE_INDEX = 6, TICKS_FROZEN_IN_POWDERED_SNOW = 7;

    /**
     * Bit masks for the {@link #ENTITY_BIT_MASK_INDEX}
     */
    byte IS_ON_FIRE_BYTE = 0x01, IS_CROUCHING = 0x02, IS_SPRINTING = 0x08, IS_SWIMMING = 0x10, IS_INVISIBLE = 0x20,
            HAS_GLOWING_EFFECT = 0x40, IS_FLYING_WITH_ELYTRA = (byte) 0x80;

    @NotNull
    UUID uuid();

    int entityId();

    boolean onFire();

    void onFire(boolean onFire);

    boolean sneaking();

    void sneaking(boolean sneaking);

    boolean sprinting();

    void sprinting(boolean sprinting);

    boolean invisible();

    void invisible(boolean invisible);

    int airTicks();

    void airTicks(int airTicks);

    boolean glowingEffect();

    void glowingEffect(boolean glowingEffect);

    boolean swimming();

    void swimming(boolean swimming);

    boolean flyingWithElytra();

    void flyingWithElytra(boolean flyingWithElytra);

    boolean silent();

    void silent(boolean silent);

    boolean noGravity();

    void noGravity(boolean noGravity);

    int ticksFrozenInPowderedSnow();

    void ticksFrozenInPowderedSnow(int ticksFrozenInPowderedSnow);

    @NotNull
    EntityPose entityPose();

    void entityPose(@NotNull EntityPose entityPose);

    Optional<Component> displayName();

    void displayName(@Nullable Component displayName);

    default void displayName(@Nullable ComponentLike displayName) {
        displayName(displayName != null ? displayName.asComponent() : null);
    }

    default void displayName(@Nullable String displayNameMiniMessage) {
        if (displayNameMiniMessage != null) {
            displayName(miniMessage().deserialize(displayNameMiniMessage));
        } else {
            displayName((Component) null);
        }
    }

    MiniMessage miniMessage();

    Optional<SurfInteractHandler<Impl>> interactHandler();

    void interactHandler(@Nullable SurfInteractHandler<Impl> interactHandler);

    boolean spawn(@NotNull Location location);

    boolean isSpawned();

    boolean despawn();

    boolean teleport(@NotNull Location location);

    boolean addViewer(@NotNull UUID uuid);

    boolean removeViewer(@NotNull UUID uuid);

    void startBatchUpdate();

    void pushBatchUpdate();

    default boolean respawn(@NotNull Location location) {
        return despawn() && spawn(location);
    }
}
