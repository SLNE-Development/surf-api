package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketLivingEntity;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityCommon;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public abstract class PacketLivingEntityImpl<Impl extends PacketLivingEntity<Impl>> extends PacketEntityImpl<Impl> implements PacketLivingEntity<Impl> {

    private float headPitch = 0;

    public PacketLivingEntityImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public float headPitch() {
        return headPitch;
    }

    @Override
    public void headPitch(float headPitch) {
        this.headPitch = headPitch;
    }

    @Override
    public boolean handActive() {
        return getMaskBit(LIVING_ENTITY_BIT_MASK_INDEX, HAND_ACTIVE_BIT);
    }

    @Override
    public void handActive(boolean handActive) {
        setMaskBit(LIVING_ENTITY_BIT_MASK_INDEX, HAND_ACTIVE_BIT, handActive);
        afterSet();
    }

    @Override
    public HumanoidArm activeHand() {
        return HumanoidArm.getById(getMaskBitRaw(LIVING_ENTITY_BIT_MASK_INDEX, ACTIVE_HAND_BIT));
    }

    @Override
    public void activeHand(@NotNull HumanoidArm activeHand) {
        setMaskBit(LIVING_ENTITY_BIT_MASK_INDEX, ACTIVE_HAND_BIT, ((byte) activeHand.getId()));
        afterSet();
    }

    @Override
    public boolean inRiptideAttack() {
        return getMaskBit(LIVING_ENTITY_BIT_MASK_INDEX, IN_RIPTIDE_ATTACK_BIT);
    }

    @Override
    public void inRiptideAttack(boolean inRiptideAttack) {
        setMaskBit(LIVING_ENTITY_BIT_MASK_INDEX, IN_RIPTIDE_ATTACK_BIT, inRiptideAttack);
        afterSet();
    }

    @Override
    public float health() {
        return get(HEALTH_INDEX, 1.0f);
    }

    @Override
    public void health(float health) {
        set(HEALTH_INDEX, health);
        afterSet();
    }

    @Override
    public TextColor potionEffectColor() {
        return TextColor.color(get(POTION_EFFECT_COLOR_INDEX, 0));
    }

    @Override
    public void potionEffectColor(@Nullable TextColor potionEffectColor) {
        set(POTION_EFFECT_COLOR_INDEX, potionEffectColor != null ? potionEffectColor.value() : 0);
        afterSet();
    }

    @Override
    public boolean potionEffectAmbient() {
        return get(POTION_EFFECT_AMBIENT_INDEX, false);
    }

    @Override
    public void potionEffectAmbient(boolean potionEffectAmbient) {
        set(POTION_EFFECT_AMBIENT_INDEX, potionEffectAmbient);
        afterSet();
    }

    @Override
    public int numberOfArrows() {
        return get(NUMBER_OF_ARROWS_INDEX, 0);
    }

    @Override
    public void numberOfArrows(int numberOfArrows) {
        set(NUMBER_OF_ARROWS_INDEX, numberOfArrows);
        afterSet();
    }

    @Override
    public int numberOfBeeStingers() {
        return get(NUMBER_OF_BEE_STINGERS_INDEX, 0);
    }

    @Override
    public void numberOfBeeStingers(int numberOfBeeStingers) {
        set(NUMBER_OF_BEE_STINGERS_INDEX, numberOfBeeStingers);
        afterSet();
    }

    @Override
    public Optional<org.spongepowered.math.vector.Vector3i> bedLocation() {
        return get(BED_LOCATION_INDEX, Optional.<Vector3i>empty()).map(PacketEntityCommon::fromPacketEvents);
    }

    @Override
    public void bedLocation(@Nullable org.spongepowered.math.vector.Vector3i bedLocation) {
        setOptBlockPos(BED_LOCATION_INDEX, Optional.ofNullable(bedLocation).map(PacketEntityCommon::toPacketEvents));
        afterSet();
    }

    @Override
    public boolean spawn(@NotNull Location location) {
        checkNotNull(location, "Cannot spawn entity at null location");

        if (isSpawned()) {
            return false;
        }

        this.location = location;
        sendPacketToAllViewers(this::spawnPacket);
        spawned = true;

        return true;
    }

    @Override
    public boolean addViewer(@NotNull UUID uuid) {
        final boolean success = viewers.add(checkNotNull(uuid, "Cannot add viewer with null uuid"));
        if (success && isSpawned()) {
            sendPacketToViewer(uuid, this::spawnPacket);
        }

        return success;
    }

    protected PacketWrapper<?> spawnPacket(ClientVersion version) {
        assert location != null : "Cannot spawn entity at null location";

        return new WrapperPlayServerSpawnLivingEntity(
                entityId(),
                uuid(),
                type,
                location,
                headPitch,
                velocityAtSpawn(),
                this.entityData(version)
        );
    }
}
