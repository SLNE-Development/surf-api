package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.display;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.BillboardConstraints;
import dev.slne.surf.surfapi.core.api.packet.entity.Brightness;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketDisplay;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.awt.Color;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector4f;

public abstract class PacketDisplayImpl<T extends PacketDisplay<T>> extends
    PacketEntityImpl<T> implements PacketDisplay<T> {

  protected static final Vector3f DEFAULT_TRANSLATION = Vector3f.ZERO;
  protected static final Vector3f DEFAULT_SCALE = Vector3f.ONE;
  protected static final Vector4f DEFAULT_ROTATION = Vector4f.UNIT_W;

  public PacketDisplayImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public int interpolationDisplay() {
    return get(INTERPOLATION_INDEX, 0);
  }

  @Override
  public void interpolationDisplay(int interpolationDisplay) {
    set(INTERPOLATION_INDEX, interpolationDisplay);
    afterSet();
  }

  @Override
  public int transformationInterpolationDisplay() {
    return get(TRANSFORMATION_INTERPOLATION_INDEX, 0);
  }

  @Override
  public void transformationInterpolationDisplay(int transformationInterpolationDisplay) {
    set(TRANSFORMATION_INTERPOLATION_INDEX, transformationInterpolationDisplay);
    afterSet();
  }

  @Override
  public int positionInterpolationDisplay() {
    return get(POSITION_INTERPOLATION_INDEX, 0);
  }

  @Override
  public void positionInterpolationDisplay(int positionInterpolationDisplay) {
    set(POSITION_INTERPOLATION_INDEX, positionInterpolationDisplay);
    afterSet();
  }

  @Override
  public Vector3f translation() {
    return get3f(TRANSLATION_INDEX, DEFAULT_TRANSLATION);
  }

  @Override
  public void translation(@NotNull org.spongepowered.math.vector.Vector3f translation) {
    set(TRANSLATION_INDEX, checkNotNull(translation, "translation"));
    afterSet();
  }

  @Override
  public Vector3f scale() {
    return get3f(SCALE_INDEX, DEFAULT_SCALE);
  }

  @Override
  public void scale(@NotNull org.spongepowered.math.vector.Vector3f scale) {
    set(SCALE_INDEX, checkNotNull(scale, "scale"));
    afterSet();
  }

  @Override
  public Vector4f rotationLeft() {
    return get4f(ROTATION_LEFT_INDEX, DEFAULT_ROTATION);
  }

  @Override
  public void rotationLeft(@NotNull Vector4f rotationLeft) {
    set(ROTATION_LEFT_INDEX, checkNotNull(rotationLeft, "rotationLeft"));
    afterSet();
  }

  @Override
  public Vector4f rotationRight() {
    return get4f(ROTATION_RIGHT_INDEX, DEFAULT_ROTATION);
  }

  @Override
  public void rotationRight(@NotNull Vector4f rotationRight) {
    set(ROTATION_RIGHT_INDEX, checkNotNull(rotationRight, "rotationRight"));
    afterSet();
  }

  @Override
  public BillboardConstraints billboardConstraints() {
    return BillboardConstraints.BY_ID.get(get(BILLBOARD_CONSTRAINTS_INDEX, (byte) 0));
  }

  @Override
  public void billboardConstraints(@NotNull BillboardConstraints billboardConstraints) {
    setByte(BILLBOARD_CONSTRAINTS_INDEX,
        checkNotNull(billboardConstraints, "billboardConstraints").id());
    afterSet();
  }

  @Override
  public Brightness brightness() {
    return Brightness.unpack(get(BRIGHTNESS_OVERRIDE_INDEX, -1));
  }

  @Override
  public void brightness(@NotNull Brightness brightness) {
    set(BRIGHTNESS_OVERRIDE_INDEX, checkNotNull(brightness, "brightness").pack());
    afterSet();
  }

  @Override
  public float viewRange() {
    return get(VIEW_RANGE_INDEX, 1.0f);
  }

  @Override
  public void viewRange(float viewRange) {
    set(VIEW_RANGE_INDEX, viewRange);
    afterSet();
  }

  @Override
  public float shadowRadius() {
    return get(SHADOW_RADIUS_INDEX, 0.0f);
  }

  @Override
  public void shadowRadius(float shadowRadius) {
    set(SHADOW_RADIUS_INDEX, shadowRadius);
    afterSet();
  }

  @Override
  public float shadowStrength() {
    return get(SHADOW_STRENGTH_INDEX, 1.0f);
  }

  @Override
  public void shadowStrength(float shadowStrength) {
    set(SHADOW_STRENGTH_INDEX, shadowStrength);
    afterSet();
  }

  @Override
  public float width() {
    return get(WIDTH_INDEX, 0.0f);
  }

  @Override
  public void width(float width) {
    set(WIDTH_INDEX, width);
    afterSet();
  }

  @Override
  public float height() {
    return get(HEIGHT_INDEX, 0.0f);
  }

  @Override
  public void height(float height) {
    set(HEIGHT_INDEX, height);
    afterSet();
  }

  @Override
  public Color glowColorOverride() {
    return new Color(get(GLOW_COLOR_OVERRIDE_INDEX, -1), true);
  }

  @Override
  public void glowColorOverride(@NotNull Color color) {
    set(GLOW_COLOR_OVERRIDE_INDEX, checkNotNull(color, "Color may not be null").getRGB());
    afterSet();
  }
}
