package dev.slne.surf.surfapi.core.api.packet.entity;

import static net.kyori.adventure.text.format.TextColor.color;

import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DyeColor {
  /**
   * Represents white dye.
   */
  WHITE(0x0, 0xF, color(0xF9FFFE), color(0xF0F0F0)),
  /**
   * Represents orange dye.
   */
  ORANGE(0x1, 0xE, color(0xF9801D), color(0xEB8844)),
  /**
   * Represents magenta dye.
   */
  MAGENTA(0x2, 0xD, color(0xC74EBD), color(0xC354CD)),
  /**
   * Represents light blue dye.
   */
  LIGHT_BLUE(0x3, 0xC, color(0x3AB3DA), color(0x6689D3)),
  /**
   * Represents yellow dye.
   */
  YELLOW(0x4, 0xB, color(0xFED83D), color(0xDECF2A)),
  /**
   * Represents lime dye.
   */
  LIME(0x5, 0xA, color(0x80C71F), color(0x41CD34)),
  /**
   * Represents pink dye.
   */
  PINK(0x6, 0x9, color(0xF38BAA), color(0xD88198)),
  /**
   * Represents gray dye.
   */
  GRAY(0x7, 0x8, color(0x474F52), color(0x434343)),
  /**
   * Represents light gray dye.
   */
  LIGHT_GRAY(0x8, 0x7, color(0x9D9D97), color(0xABABAB)),
  /**
   * Represents cyan dye.
   */
  CYAN(0x9, 0x6, color(0x169C9C), color(0x287697)),
  /**
   * Represents purple dye.
   */
  PURPLE(0xA, 0x5, color(0x8932B8), color(0x7B2FBE)),
  /**
   * Represents blue dye.
   */
  BLUE(0xB, 0x4, color(0x3C44AA), color(0x253192)),
  /**
   * Represents brown dye.
   */
  BROWN(0xC, 0x3, color(0x835432), color(0x51301A)),
  /**
   * Represents green dye.
   */
  GREEN(0xD, 0x2, color(0x5E7C16), color(0x3B511A)),
  /**
   * Represents red dye.
   */
  RED(0xE, 0x1, color(0xB02E26), color(0xB3312C)),
  /**
   * Represents black dye.
   */
  BLACK(0xF, 0x0, color(0x1D1D21), color(0x1E1B1B));

  private static final DyeColor[] BY_WOOL_DATA = new DyeColor[values().length];
  private static final DyeColor[] BY_DYE_DATA = new DyeColor[values().length];
  private static final Int2ObjectMap<DyeColor> BY_COLOR;
  private static final Int2ObjectMap<DyeColor> BY_FIREWORK;

  static {
    BY_COLOR = Util.byIdMap(DyeColor.class, value -> value.getColor().value());
    BY_FIREWORK = Util.byIdMap(DyeColor.class, value -> value.getFireworkColor().value());

    for (DyeColor color : values()) {
      BY_WOOL_DATA[color.woolData & 0xff] = color;
      BY_DYE_DATA[color.dyeData & 0xff] = color;
    }
  }

  private final byte woolData;
  private final byte dyeData;
  private final TextColor color;
  private final TextColor firework;

  DyeColor(int woolData, int dyeData, TextColor color, TextColor firework) {
    this.woolData = (byte) woolData;
    this.dyeData = (byte) dyeData;
    this.color = color;
    this.firework = firework;
  }

  @Contract(pure = true)
  @ApiStatus.Internal
  public static @Nullable DyeColor getByWoolData(byte data) {
    int i = 0xff & data;
    return i >= BY_WOOL_DATA.length ? null : BY_WOOL_DATA[i];
  }

  public static @Nullable DyeColor getByColor(@NotNull TextColor color) {
    return BY_COLOR.get(color.value());
  }

  public static @Nullable DyeColor getByFireworkColor(@NotNull TextColor color) {
    return BY_FIREWORK.get(color.value());
  }

  public static @Nullable DyeColor getById(int id) {
    return id >= BY_DYE_DATA.length ? null : BY_DYE_DATA[id];
  }

  @ApiStatus.Internal
  public byte getWoolData() {
    return this.woolData;
  }

  @Contract(pure = true)
  public @NotNull TextColor getColor() {
    return this.color;
  }

  @Contract(pure = true)
  public @NotNull TextColor getFireworkColor() {
    return this.firework;
  }
}
