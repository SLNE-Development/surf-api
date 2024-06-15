package dev.slne.surf.surfapi.core.api.packet.entity.entities.display;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.core.api.packet.entity.TextAlignment;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketTextDisplay extends PacketDisplay<PacketTextDisplay>, Spawnable {

  int TEXT_INDEX = 23, LINE_WIDTH_INDEX = 24, BACKGROUND_COLO_INDEX = 25, TEXT_OPACITY_INDEX = 26,
      TEXT_BIT_MASK_INDEX = 27;

  byte HAS_SHADOW_BIT = 0x01, SEE_THROUGH_BIT = 0x02, DEFAULT_BACKGROUND_COLOR_BIT = 0x04, ALIGNMENT_BIT = 0x08;

  @NotNull
  Component text();

  void text(@NotNull Component text);

  default void text(@NotNull ComponentLike text) {
    text(checkNotNull(text, "Text may not be null").asComponent());
  }

  default void text(@NotNull String minimessageString) {
    text(miniMessage().deserialize(checkNotNull(minimessageString, "Text string may not be null")));
  }

  int lineWidth();

  void lineWidth(int lineWidth);

  @NotNull
  TextColor backgroundColor();

  void backgroundColor(@NotNull TextColor backgroundColor);

  byte textOpacity();

  void textOpacity(byte textOpacity);

  boolean shadow();

  void shadow(boolean shadow);

  boolean seeThrough();

  void seeThrough(boolean seeThrough);

  boolean defaultBackgroundColor();

  void defaultBackgroundColor(boolean defaultBackgroundColor);

  TextAlignment alignment();

  void alignment(TextAlignment alignment);
}
