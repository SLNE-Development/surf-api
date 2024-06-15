package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketParrot extends PacketTameableAnimal<PacketParrot>, Spawnable {

  int VARIANT_INDEX = 19;

  /**
   * Get the variant of this parrot.
   *
   * @return parrot variant
   */
  Variant variant();

  /**
   * Set the variant of this parrot.
   *
   * @param variant parrot variant
   */
  void variant(@NotNull Variant variant);

  /**
   * Represents the variant of a parrot - ie its color.
   */
  enum Variant {
    /**
     * Classic parrot - red with colored wingtips.
     */
    RED,
    /**
     * Royal blue colored parrot.
     */
    BLUE,
    /**
     * Green colored parrot.
     */
    GREEN,
    /**
     * Cyan colored parrot.
     */
    CYAN,
    /**
     * Gray colored parrot.
     */
    GRAY;

    public static final Int2ObjectMap<Variant> BY_ID = Util.byIdMap(Variant.class,
        Variant::ordinal);

    @Contract(pure = true)
    public int getId() {
      return ordinal();
    }
  }
}
