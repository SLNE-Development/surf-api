package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.ById;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketEnderDragon extends PacketMob<PacketEnderDragon>, Spawnable {

  int PHASE_INDEX = 16;

  /**
   * Gets the current phase that the dragon is performing.
   *
   * @return the current phase
   */
  @NotNull
  Phase phase();

  /**
   * Sets the next phase for the dragon to perform.
   *
   * @param phase the next phase
   */
  void phase(@NotNull Phase phase);

  /**
   * Represents a phase or action that an Ender Dragon can perform.
   */
  enum Phase implements ById {
    /**
     * The dragon will circle outside the ring of pillars if ender crystals remain or inside the
     * ring if not.
     */
    CIRCLING,
    /**
     * The dragon will fly towards a targeted player and shoot a fireball when within 64 blocks.
     */
    STRAFING,
    /**
     * The dragon will fly towards the empty portal (approaching from the other side, if
     * applicable).
     */
    FLY_TO_PORTAL,
    /**
     * The dragon will land on the portal. If the dragon is not near the portal, it will fly to it
     * before mounting.
     */
    LAND_ON_PORTAL,
    /**
     * The dragon will leave the portal.
     */
    LEAVE_PORTAL,
    /**
     * The dragon will attack with dragon breath at its current location.
     */
    BREATH_ATTACK,
    /**
     * The dragon will search for a player to attack with dragon breath. If no player is close
     * enough to the dragon for 5 seconds, the dragon will charge at a player within 150 blocks or
     * will take off and begin circling if no player is found.
     */
    SEARCH_FOR_BREATH_ATTACK_TARGET,
    /**
     * The dragon will roar before performing a breath attack.
     */
    ROAR_BEFORE_ATTACK,
    /**
     * The dragon will charge a player.
     */
    CHARGE_PLAYER,
    /**
     * The dragon will fly to the vicinity of the portal and die.
     */
    DYING,
    /**
     * The dragon will hover at its current location, not performing any actions.
     */
    HOVER;

    public static final Int2ObjectMap<Phase> BY_ID = ById.build(Phase.class);

    @Contract(pure = true)
    @Override
    public int id() {
      return ordinal();
    }
  }
}
