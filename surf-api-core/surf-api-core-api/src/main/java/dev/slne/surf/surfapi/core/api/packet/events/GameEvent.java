package dev.slne.surf.surfapi.core.api.packet.events;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import dev.slne.surf.surfapi.core.api.util.ById;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jetbrains.annotations.Range;

public final class GameEvent {

  /**
   * Displays message 'block.minecraft.spawn.not_valid' (You have no home bed or charged respawn
   * anchor, or it was obstructed) to the player.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType NO_RESPAWN_BLOCK_AVAILABLE = simpleType(0);

  /**
   * This event is triggered when it starts raining in the game.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType START_RAINING = simpleType(1);

  /**
   * This event is triggered when it stops raining in the game.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType STOP_RAINING = simpleType(2);

  /**
   * This event is triggered when the game mode is changed.
   */
  public static final ChangeGameModeType CHANGE_GAME_MODE = new ChangeGameModeType();

  /**
   * This event is triggered when the game is won.
   */
  public static final WinGameType WIN_GAME = new WinGameType();

  /**
   * This event is triggered during a demo event.
   */
  public static final DemoEventType DEMO_EVENT = new DemoEventType();

  /**
   * Sent when any player is struck by an arrow.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType ARROW_HIT_PLAYER = simpleType(6);

  /**
   * Note: Seems to change both sky color and lighting.
   * <br>
   * Rain level ranging from 0 to 1.
   */
  @Range(from = 0L, to = 1L)
  public static final SimpleType RAIN_LEVEL_CHANGE = simpleType(7);

  /**
   * Note: Seems to change both sky color and lighting (same as Rain level change, but doesn't start
   * rain). It also requires rain to render by notchian client.
   * <br>
   * Thunder level ranging from 0 to 1.
   */
  @Range(from = 0L, to = 1L)
  public static final SimpleType THUNDER_LEVEL_CHANGE = simpleType(8);

  /**
   * This event is triggered when a player is stung by a puffer fish.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType PUFFER_FISH_STING = simpleType(9);

  /**
   * This event is triggered when a player is affected by the elder guardian effect.
   */
  @ValueCanBeNull
  @Range(from = 0L, to = 0L)
  public static final SimpleType GUARDIAN_ELDER_EFFECT = simpleType(10);

  /**
   * {@code false}: Enable respawn screen.
   * <br>
   * {@code true}: Immediately respawn (sent when the doImmediateRespawn gamerule changes).
   */
  public static final SimpleBooleanType IMMEDIATE_RESPAWN = simpleBooleanType(11);

  /**
   * {@code false}: Disable limited crafting.
   * <br>
   * {@code true}: Enable limited crafting (sent when the doLimitedCrafting gamerule changes).
   */
  public static final SimpleBooleanType LIMITED_CRAFTING = simpleBooleanType(12);

  /**
   * Instructs the client to begin the waiting process for the level chunks. Sent by the server
   * after the level is cleared on the client and is being re-sent (either during the first, or
   * subsequent reconfigurations).
   */
  @Range(from = 0L, to = 0L)
  public static final SimpleType START_WAIT_FOR_CHUNKS = simpleType(13);

  private static SimpleType simpleType(int id) {
    return new SimpleType(id);
  }

  private static SimpleBooleanType simpleBooleanType(int id) {
    return new SimpleBooleanType(id);
  }

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({ElementType.FIELD})
  public @interface ValueCanBeNull {

  }

  public abstract static class Type<T> {

    private final int id;

    public Type(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public abstract float encode(T value);
  }

  public static final class SimpleType extends Type<Float> {

    public SimpleType(int id) {
      super(id);
    }

    @Override
    public float encode(Float value) {
      return value;
    }
  }

  public static final class SimpleBooleanType extends Type<Boolean> {

    public SimpleBooleanType(int id) {
      super(id);
    }

    @Override
    public float encode(Boolean value) {
      return value ? 1 : 0;
    }
  }

  public static final class ChangeGameModeType extends Type<GameMode> {

    public ChangeGameModeType() {
      super(3);
    }

    @Override
    public float encode(GameMode value) {
      return value.getId();
    }
  }

  public static final class WinGameType extends Type<WinGameType.Action> {

    WinGameType() {
      super(4);
    }

    @Override
    public float encode(Action value) {
      return value.ordinal();
    }

    public enum Action {
      /**
       * Just respawn player.
       */
      ONLY_RESPAWN,

      /**
       * Roll the credits and respawn player.
       */
      SHOW_CREDITS
    }
  }

  public static final class DemoEventType extends Type<DemoEventType.Action> {

    DemoEventType() {
      super(5);
    }

    @Override
    public float encode(Action value) {
      return value.id();
    }

    public enum Action implements ById {
      WELCOME_SCREEN(0),
      TELL_MOVEMENT_CONTROLS(101),
      TELL_JUMP_CONTROL(102),
      TELL_INVENTORY_CONTROL(103),
      TELL_DEMO_OVER(104),
      ;

      private final int id;

      Action(int id) {
        this.id = id;
      }

      @Override
      public int id() {
        return id;
      }
    }
  }
}
