package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.FinePosition;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.function.UnaryOperator;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.AccessLevel;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.imaginary.Quaternionf;
import org.spongepowered.math.vector.Vector3f;

@SuppressWarnings("UnstableApiUsage")
@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsSpawnPackets {

  PacketOperation despawn(IntList entityIds);

  PacketOperation despawn(int... entityIds);

  PacketOperation spawnItemDisplay(
      int entityId,
      FinePosition position,
      ItemDisplaySettings settings
  );

  default PacketOperation spawnItemDisplay(
      int entityId,
      FinePosition position,
      UnaryOperator<ItemDisplaySettings.ItemDisplaySettingsBuilder<?, ?>> settings
  ) {
    return spawnItemDisplay(entityId, position,
        settings.apply(ItemDisplaySettings.builder()).build());
  }

  PacketOperation spawnTextDisplay(
      int entityId,
      FinePosition position,
      TextDisplaySettings settings
  );

  default PacketOperation spawnTextDisplay(
      int entityId,
      FinePosition position,
      UnaryOperator<TextDisplaySettings.TextDisplaySettingsBuilder<?, ?>> settings
  ) {
    return spawnTextDisplay(entityId, position,
        settings.apply(TextDisplaySettings.builder()).build());
  }

  PacketOperation updateSign(
      int entityId,
      BlockPosition position,
      SignBlockUpdateSettings settings
  );

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  abstract class DisplaySettings {

    private float pitch, yaw;
    private @Nullable Vector3f translation, scale;
    private @Nullable Quaternionf leftRotation, rightRotation;
    private @Default Billboard billboardConstraints = Billboard.FIXED;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  class ItemDisplaySettings extends DisplaySettings {

    private @Default ItemStack itemStack = ItemStack.empty();
    private @Default ItemDisplayTransform itemDisplayTransform = ItemDisplayTransform.NONE;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  class TextDisplaySettings extends DisplaySettings {

    private Component text = Component.empty();
    private int lineWidth = 200;
    private TextColor backgroundColor = TextColor.color(0x40000000);
    private TextAlignment textAlignment = TextAlignment.CENTER;
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @NonExtendable
  @Setter
  class SignBlockUpdateSettings {

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

  static SurfBukkitNmsSpawnPackets get() {
    return SurfBukkitNmsPacketBridges.get().getSpawnPackets();
  }
}
