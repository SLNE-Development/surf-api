package dev.slne.surf.surfapi.core.api.packet.entity.entities.living;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.pos.rot.PreciseRotation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.*;

@CanBeSpawned
public interface PacketArmorStand extends PacketLivingEntity<PacketArmorStand>, Spawnable {

    int ARMOR_STAND_BIT_MASK_INDEX = 15, HEAD_ROTATION_INDEX = 16, BODY_ROTATION_INDEX = 17,
            LEFT_ARM_ROTATION_INDEX = 18, RIGHT_ARM_ROTATION_INDEX = 19, LEFT_LEG_ROTATION_INDEX = 20,
            RIGHT_LEG_ROTATION_INDEX = 21;

    byte SMALL_BIT = 0x01, SHOW_ARMS_BIT = 0x04, NO_BASE_PLATE_BIT = 0x08, MARKER_BIT = 0x10;

    boolean small();

    void small(boolean small);

    boolean showArms();

    void showArms(boolean showArms);

    boolean noBasePlate();

    void noBasePlate(boolean noBasePlate);

    boolean marker();

    void marker(boolean marker);

    PreciseRotation headRotation();

    void headRotation(@NotNull PreciseRotation headRotation);

    PreciseRotation bodyRotation();

    void bodyRotation(@NotNull PreciseRotation bodyRotation);

    PreciseRotation leftArmRotation();

    void leftArmRotation(@NotNull PreciseRotation leftArmRotation);

    PreciseRotation rightArmRotation();

    void rightArmRotation(@NotNull PreciseRotation rightArmRotation);

    PreciseRotation leftLegRotation();

    void leftLegRotation(@NotNull PreciseRotation leftLegRotation);

    PreciseRotation rightLegRotation();

    void rightLegRotation(@NotNull PreciseRotation rightLegRotation);

    // TODO: provide some cool default poses

    /**
     * Some default armor stand poses
     */
    @ApiStatus.Experimental
    interface Poses {

        // TODO: add photos of the poses
        Poses MINING_DWARF = (armorStand, onlyModifyRotation, pushBatchUpdate) ->
                Internal.handle(armorStand, onlyModifyRotation, pushBatchUpdate, () -> {
                    armorStand.noBasePlate(true);
                    armorStand.showArms(true);
                    // TODO: add equipment
                }, () -> {
                    armorStand.bodyRotation(PreciseRotation.of(12, 0, 0));
                    armorStand.headRotation(PreciseRotation.of(33, 2, 0));
                    armorStand.leftLegRotation(PreciseRotation.of(0, 0, 359));
                    armorStand.rightLegRotation(PreciseRotation.of(0, 0, 1));
                    armorStand.leftArmRotation(PreciseRotation.of(339, 0, 344));
                    armorStand.rightArmRotation(PreciseRotation.of(331, 0, 0));
                });

        Poses ARCHER_GUARD = (armorStand, onlyModifyRotation, pushBatchUpdate) ->
                Internal.handle(armorStand, onlyModifyRotation, pushBatchUpdate, () -> {
                    armorStand.noBasePlate(true);
                    armorStand.showArms(true);
                    // TODO: add equipment
                }, () -> {
                    armorStand.bodyRotation(PreciseRotation.of(7, 16, 0));
                    armorStand.headRotation(PreciseRotation.of(3, 13, 0));
                    armorStand.leftLegRotation(PreciseRotation.of(0, 0, 359));
                    armorStand.rightLegRotation(PreciseRotation.of(0, 0, 1));
                    armorStand.leftArmRotation(PreciseRotation.of(334, 1, 0));
                    armorStand.rightArmRotation(PreciseRotation.of(276, 360, 27));
                });

        /**
         * Applies the pose to the provided armor stand with the given options.
         *
         * @param armorStand         the armor stand to apply the pose to
         * @param onlyModifyRotation if only the rotation should be modified and not e.g.,
         *                           attributes or equipment of the armor stand
         * @param pushBatchUpdate    if the pose should be applied immediately (Set to {@code false}
         *                           if the entity is already in {@link PacketEntity#startBatchUpdate()}
         */
        void apply(PacketArmorStand armorStand, boolean onlyModifyRotation, boolean pushBatchUpdate);

        /**
         * Applies the pose to the provided armor stand immediately with the given option.
         *
         * @param armorStand         the armor stand to apply the pose to
         * @param onlyModifyRotation if only the rotation should be modified and not e.g.,
         *                           attributes or equipment of the armor stand
         * @see #apply(PacketArmorStand, boolean, boolean)
         */
        default void apply(PacketArmorStand armorStand, boolean onlyModifyRotation) {
            apply(armorStand, onlyModifyRotation, true);
        }

        /**
         * Applies the pose without any modifications other than rotation to the provided armor stand immediately.
         *
         * @param armorStand the armor stand to apply the pose to
         * @see #apply(PacketArmorStand, boolean)
         */
        default void apply(PacketArmorStand armorStand) {
            apply(armorStand, false, true);
        }

        class Internal {
            static void handle(PacketArmorStand armorStand,
                               boolean onlyModifyRotation,
                               boolean pushBatchUpdate,
                               Runnable otherModifications,
                               Runnable rotationModification) {
                checkNotNull(armorStand, "Provided armor stand may not be null");
                armorStand.startBatchUpdate();

                if (!onlyModifyRotation) {
                    otherModifications.run();
                }

                rotationModification.run();

                if (pushBatchUpdate) {
                    armorStand.pushBatchUpdate();
                }
            }
        }
    }
}
