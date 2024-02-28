package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketTropicalFish extends PacketAbstractFish<PacketTropicalFish>, Spawnable {

  int VARIANT_INDEX = 17;

  Pattern variant();

  void variant(@NotNull Pattern variant);

  DyeColor patternColor();

  void patternColor(@NotNull DyeColor patternColor);

  DyeColor bodyColor();

  void bodyColor(@NotNull DyeColor bodyColor);

  enum Pattern {
    KOB(0, false),
    SUNSTREAK(1, false),
    SNOOPER(2, false),
    DASHER(3, false),
    BRINELY(4, false),
    SPOTTY(5, false),
    FLOPPER(0, true),
    STRIPEY(1, true),
    GLITTER(2, true),
    BLOCKFISH(3, true),
    BETTY(4, true),
    CLAYFISH(5, true);

    private static final Int2ObjectMap<Pattern> BY_DATA;

    static {
      BY_DATA = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(
          Arrays.stream(values())
              .collect(Collectors.toMap(Pattern::getDataValue, Function.identity()))
      ));
    }

    private final int variant;
    private final boolean large;

    Pattern(int variant, boolean large) {
      this.variant = variant;
      this.large = large;
    }

    public static Pattern getByData(int data) {
      return BY_DATA.get(data);
    }

    public int getDataValue() {
      return this.variant << 8 | ((this.large) ? 1 : 0);
    }
  }
}
