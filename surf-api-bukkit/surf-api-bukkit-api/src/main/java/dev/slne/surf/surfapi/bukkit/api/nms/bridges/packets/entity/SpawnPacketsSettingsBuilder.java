package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity;

import lombok.AccessLevel;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.imaginary.Quaternionf;
import org.spongepowered.math.vector.Vector3f;

public final class SpawnPacketsSettingsBuilder {

  private SpawnPacketsSettingsBuilder() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  abstract static class DisplaySettings {

    private float pitch, yaw;
    private @Nullable Vector3f translation, scale;
    private @Nullable Quaternionf leftRotation, rightRotation;
    private @Default Billboard billboardConstraints = Billboard.FIXED;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  public static class ItemDisplaySettings extends DisplaySettings {

    private @Default ItemStack itemStack = ItemStack.empty();
    private @Default ItemDisplayTransform itemDisplayTransform = ItemDisplayTransform.NONE;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  public static class TextDisplaySettings extends DisplaySettings {

    private @Default Component text = Component.empty();
    private @Default int lineWidth = 200;
    private @Default TextColor backgroundColor = TextColor.color(0x40000000);
    private @Default TextAlignment textAlignment = TextAlignment.CENTER;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  public static class BlockDisplaySettings extends DisplaySettings {

    private @Default BlockData blockData = Material.AIR.createBlockData();
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  @Setter
  @ToString
  public static class SignBlockUpdateSettings {

    private @NotNull
    @Default
    SignText frontText = SignText.empty(), backText = SignText.empty();

    public void setText(SignText text, boolean front) {
      if (front) {
        setFrontText(text);
      } else {
        setBackText(text);
      }
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @NonExtendable
    @ToString
    public static class SignText {

      private @NotNull Component line1, line2, line3, line4;

      public static SignText empty() {
        return SignText.builder()
            .line1(Component.empty())
            .line2(Component.empty())
            .line3(Component.empty())
            .line4(Component.empty())
            .build();
      }
    }
  }
}
