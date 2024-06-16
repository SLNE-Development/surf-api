package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import io.papermc.paper.math.FinePosition;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.spongepowered.math.imaginary.Quaternionf;
import org.spongepowered.math.vector.Vector3f;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsSpawnPackets {

  PacketOperation despawn(int... entityIds);

  PacketOperation spawnItemDisplay(int entityId, FinePosition position,
      Consumer<ItemDisplaySettings> settingsConsumer);

  PacketOperation spawnTextDisplay(int entityId, FinePosition position,
      Consumer<TextDisplaySettings> settingsConsumer);

  abstract class DisplaySettings {

    private float pitch, yaw;
    private Vector3f translation, scale;
    private Quaternionf leftRotation, rightRotation;
    private Billboard billboardConstraints = Billboard.FIXED;

    public Billboard getBillboardConstraints() {
      return billboardConstraints;
    }

    public DisplaySettings setBillboardConstraints(
        Billboard billboardConstraints) {
      this.billboardConstraints = billboardConstraints;
      return this;
    }

    public Quaternionf getLeftRotation() {
      return leftRotation;
    }

    public DisplaySettings setLeftRotation(Quaternionf leftRotation) {
      this.leftRotation = leftRotation;
      return this;
    }

    public float getPitch() {
      return pitch;
    }

    public DisplaySettings setPitch(float pitch) {
      this.pitch = pitch;
      return this;
    }

    public Quaternionf getRightRotation() {
      return rightRotation;
    }

    public DisplaySettings setRightRotation(
        Quaternionf rightRotation) {
      this.rightRotation = rightRotation;
      return this;
    }

    public Vector3f getScale() {
      return scale;
    }

    public DisplaySettings setScale(Vector3f scale) {
      this.scale = scale;
      return this;
    }

    public Vector3f getTranslation() {
      return translation;
    }

    public DisplaySettings setTranslation(Vector3f translation) {
      this.translation = translation;
      return this;
    }

    public float getYaw() {
      return yaw;
    }

    public DisplaySettings setYaw(float yaw) {
      this.yaw = yaw;
      return this;
    }
  }

  class ItemDisplaySettings extends DisplaySettings {

    private ItemStack itemStack = ItemStack.empty();
    private ItemDisplayTransform itemDisplayTransform = ItemDisplayTransform.NONE;

    public ItemDisplaySettings() {
    }


    public ItemStack getItemStack() {
      return itemStack;
    }

    public ItemDisplaySettings setItemStack(ItemStack itemStack) {
      this.itemStack = itemStack;
      return this;
    }

    public ItemDisplayTransform getItemDisplayTransform() {
      return itemDisplayTransform;
    }

    public ItemDisplaySettings setItemDisplayTransform(
        ItemDisplayTransform itemDisplayTransform) {
      this.itemDisplayTransform = itemDisplayTransform;
      return this;
    }
  }

  class TextDisplaySettings extends DisplaySettings {

    private Component text = Component.empty();
    private int lineWidth = 200;
    private TextColor backgroundColor = TextColor.color(0x40000000);
    private TextAlignment textAlignment = TextAlignment.CENTER;

    public Component getText() {
      return text;
    }

    public TextDisplaySettings setText(Component text) {
      this.text = text;
      return this;
    }

    public int getLineWidth() {
      return lineWidth;
    }

    public TextDisplaySettings setLineWidth(int lineWidth) {
      this.lineWidth = lineWidth;
      return this;
    }

    public TextColor getBackgroundColor() {
      return backgroundColor;
    }

    public TextDisplaySettings setBackgroundColor(
        TextColor backgroundColor) {
      this.backgroundColor = backgroundColor;
      return this;
    }

    public TextAlignment getTextAlignment() {
      return textAlignment;
    }

    public TextDisplaySettings setTextAlignment(
        TextAlignment textAlignment) {
      this.textAlignment = textAlignment;
      return this;
    }
  }

  static SurfBukkitNmsSpawnPackets get() {
    return SurfBukkitNmsPacketBridges.get().getSpawnPackets();
  }
}
