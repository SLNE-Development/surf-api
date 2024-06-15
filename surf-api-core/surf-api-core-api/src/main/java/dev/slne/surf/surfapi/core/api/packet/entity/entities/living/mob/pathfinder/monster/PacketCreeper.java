package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketCreeper extends PacketMonster<PacketCreeper>, Spawnable {

  int STATE_INDEX = 16, POWERED_INDEX = 17, IGNITED_INDEX = 18;

  /**
   * Gets the current state of this Creeper
   *
   * @return Current state
   */
  State state();

  /**
   * Sets the current state of this Creeper
   *
   * @param state New state
   */
  void state(@NotNull State state);

  /**
   * Checks if this Creeper is powered (Electrocuted)
   *
   * @return true if this creeper is powered
   */
  boolean powered();

  /**
   * Sets the Powered status of this Creeper
   *
   * @param value New Powered status
   */
  void powered(boolean value);

  /**
   * Check if creeper is ignited or not (armed to explode)
   *
   * @return Ignited state
   */
  boolean ignited();

  /**
   * Set whether creeper is ignited or not (armed to explode)
   *
   * @param ignited New ignited state
   */
  void ignited(boolean ignited);

  enum State {
    IDLE(-1),
    FUSE(1);

    public static final Int2ObjectMap<State> BY_ID = Util.byIdMap(State.class, State::getId);
    private final int id;

    @Contract(pure = true)
    State(int id) {
      this.id = id;
    }

    @Contract(pure = true)
    public int getId() {
      return id;
    }
  }
}
