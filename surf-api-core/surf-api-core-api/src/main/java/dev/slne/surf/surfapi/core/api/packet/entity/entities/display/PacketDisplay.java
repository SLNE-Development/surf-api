package dev.slne.surf.surfapi.core.api.packet.entity.entities.display;

import com.github.retrooper.packetevents.util.Quaternion4f;
import dev.slne.surf.surfapi.core.api.packet.entity.BillboardConstraints;
import dev.slne.surf.surfapi.core.api.packet.entity.Brightness;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector4f;

import java.awt.*;

@ApiStatus.NonExtendable
public interface PacketDisplay<Impl extends PacketDisplay<Impl>> extends PacketEntity<Impl> {

    /**
     * Index numbers for packets
     */
    int INTERPOLATION_INDEX = 8, TRANSFORMATION_INTERPOLATION_INDEX = 9, POSITION_INTERPOLATION_INDEX = 10, TRANSLATION_INDEX = 11,
            SCALE_INDEX = 12, ROTATION_LEFT_INDEX = 13, ROTATION_RIGHT_INDEX = 14, BILLBOARD_CONSTRAINTS_INDEX = 15,
            BRIGHTNESS_OVERRIDE_INDEX = 16, VIEW_RANGE_INDEX = 17, SHADOW_RADIUS_INDEX = 18, SHADOW_STRENGTH_INDEX = 19,
            WIDTH_INDEX = 20, HEIGHT_INDEX = 21, GLOW_COLOR_OVERRIDE_INDEX = 22;

    int interpolationDisplay();

    void interpolationDisplay(int interpolationDisplay);

    int transformationInterpolationDisplay();

    void transformationInterpolationDisplay(int transformationInterpolationDisplay);

    int positionInterpolationDisplay();

    void positionInterpolationDisplay(int positionInterpolationDisplay);

    Vector3f translation();

    void translation(@NotNull Vector3f translation);

    Vector3f scale();

    void scale(@NotNull Vector3f scale);

    Vector4f rotationLeft();

    void rotationLeft(@NotNull Vector4f rotationLeft);

    Vector4f rotationRight();

    void rotationRight(@NotNull Vector4f rotationRight);

    BillboardConstraints billboardConstraints();

    void billboardConstraints(@NotNull BillboardConstraints billboardConstraints);

    Brightness brightness();

    void brightness(@NotNull Brightness brightness);

    float viewRange();

    void viewRange(float viewRange);

    float shadowRadius();

    void shadowRadius(float shadowRadius);

    float shadowStrength();

    void shadowStrength(float shadowStrength);

    float width();

    void width(float width);

    float height();

    void height(float height);

    Color glowColorOverride(); // TODO: does the color from java work?

    void glowColorOverride(@NotNull Color color);
}
