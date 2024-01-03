package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketArmorStand;
import dev.slne.surf.surfapi.core.api.util.pos.rot.PreciseRotation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketArmorStandImpl extends PacketLivingEntityImpl<PacketArmorStand> implements PacketArmorStand {

    public PacketArmorStandImpl(UUID uuid) {
        super(uuid, EntityTypes.ARMOR_STAND);
    }

    @Override
    public boolean small() {
        return getMaskBit(ARMOR_STAND_BIT_MASK_INDEX, SMALL_BIT);
    }

    @Override
    public void small(boolean small) {
        setMaskBit(ARMOR_STAND_BIT_MASK_INDEX, SMALL_BIT, small);
        afterSet();
    }

    @Override
    public boolean showArms() {
        return getMaskBit(ARMOR_STAND_BIT_MASK_INDEX, SHOW_ARMS_BIT);
    }

    @Override
    public void showArms(boolean showArms) {
        setMaskBit(ARMOR_STAND_BIT_MASK_INDEX, SHOW_ARMS_BIT, showArms);
        afterSet();
    }

    @Override
    public boolean noBasePlate() {
        return getMaskBit(ARMOR_STAND_BIT_MASK_INDEX, SHOW_ARMS_BIT);
    }

    @Override
    public void noBasePlate(boolean noBasePlate) {
        setMaskBit(ARMOR_STAND_BIT_MASK_INDEX, NO_BASE_PLATE_BIT, noBasePlate);
        afterSet();
    }

    @Override
    public boolean marker() {
        return getMaskBit(ARMOR_STAND_BIT_MASK_INDEX, MARKER_BIT);
    }

    @Override
    public void marker(boolean marker) {
        setMaskBit(ARMOR_STAND_BIT_MASK_INDEX, MARKER_BIT, marker);
        afterSet();
    }

    @Override
    public PreciseRotation headRotation() {
        return PreciseRotation.fromPacketEvents(get(HEAD_ROTATION_INDEX, Vector3f.zero()));
    }

    @Override
    public void headRotation(@NotNull PreciseRotation headRotation) {
        set(HEAD_ROTATION_INDEX, checkNotNull(headRotation, "Head rotation may not be null"));
        afterSet();
    }

    @Override
    public PreciseRotation bodyRotation() {
        return PreciseRotation.fromPacketEvents(get(BODY_ROTATION_INDEX, Vector3f.zero()));
    }

    @Override
    public void bodyRotation(@NotNull PreciseRotation bodyRotation) {
        set(BODY_ROTATION_INDEX, checkNotNull(bodyRotation, "Body rotation may not be null"));
        afterSet();
    }

    @Override
    public PreciseRotation leftArmRotation() {
        return PreciseRotation.fromPacketEvents(get(LEFT_ARM_ROTATION_INDEX, new Vector3f(-10, 0, -10)));
    }

    @Override
    public void leftArmRotation(@NotNull PreciseRotation leftArmRotation) {
        set(LEFT_LEG_ROTATION_INDEX, checkNotNull(leftArmRotation, "Left arm rotation may not be null"));
        afterSet();
    }

    @Override
    public PreciseRotation rightArmRotation() {
        return PreciseRotation.fromPacketEvents(get(RIGHT_ARM_ROTATION_INDEX, new Vector3f(-15, 0, -10)));
    }

    @Override
    public void rightArmRotation(@NotNull PreciseRotation rightArmRotation) {
        set(RIGHT_ARM_ROTATION_INDEX, checkNotNull(rightArmRotation, "Right arm rotation may not be null"));
        afterSet();
    }

    @Override
    public PreciseRotation leftLegRotation() {
        return PreciseRotation.fromPacketEvents(get(LEFT_LEG_ROTATION_INDEX, new Vector3f(-1, 0, -1)));
    }

    @Override
    public void leftLegRotation(@NotNull PreciseRotation leftLegRotation) {
        set(LEFT_LEG_ROTATION_INDEX, checkNotNull(leftLegRotation, "Left leg rotation may not be null"));
        afterSet();
    }

    @Override
    public PreciseRotation rightLegRotation() {
        return PreciseRotation.fromPacketEvents(get(RIGHT_LEG_ROTATION_INDEX, new Vector3f(1, 0, 1)));
    }

    @Override
    public void rightLegRotation(@NotNull PreciseRotation rightLegRotation) {
        set(RIGHT_LEG_ROTATION_INDEX, checkNotNull(rightLegRotation, "Right leg rotation may not be null"));
        afterSet();
    }
}
