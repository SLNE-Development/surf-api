package dev.slne.surf.surfapi.core.api.packet.entity.entities.living;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketPlayer extends PacketLivingEntity<PacketPlayer>,
    Spawnable { // TODO: NPC functions

  int ADDITIONAL_HEARTS_INDEX = 15, SCORE_INDEX = 16, DISPLAY_SKIN_PART_BIT_MASK_INDEX = 17, MAIN_HAND_INDEX = 18,
      LEFT_SHOULDER_DATA_INDEX = 19, RIGHT_SHOULDER_DATA_INDEX = 20;

  byte CAPE_ENABLED_BIT = 0x01, JACKET_ENABLED_BIT = 0x02, LEFT_SLEEVE_ENABLED_BIT = 0x04,
      RIGHT_SLEEVE_ENABLED_BIT = 0x08, LEFT_PANTS_LEG_ENABLED_BIT = 0x10, RIGHT_PANTS_LEG_ENABLED_BIT = 0x20,
      HAT_ENABLED_BIT = 0x40, UNUSED_ENABLED_BIT = (byte) 0x80;

  List<TextureProperty> skinProperties();

  @NeedsRespawn
  void skinProperties(@NotNull List<TextureProperty> skinProperties);

  /**
   * Fetches the skin properties asynchronously and calls {@link #skinProperties(List)} when done.
   *
   * @param playerUuid The UUID of the player to fetch the skin properties of.
   * @param respawn    Whether to respawn the player after the skin properties have been fetched to
   *                   apply the skin.
   * @return A {@link CompletableFuture} that completes when the skin properties have been fetched
   * and {@link #skinProperties(List)} has been called.
   */
  CompletableFuture<Void> skinProperties(@NotNull UUID playerUuid, boolean respawn);

  default void changeSkin(List<TextureProperty> skinProperties) {
    checkNotNull(skinProperties, "skinProperties");

    skinProperties(skinProperties);
    respawn();
  }

  float additionalHearts();

  void additionalHearts(float additionalHearts);

  boolean showOnTabList();

  void showOnTabList(boolean showOnTabList);

  Ping ping();

  void ping(@NotNull Ping ping);

  GameMode gameMode();

  void gameMode(@NotNull GameMode gameMode);

  int score();

  void score(int score);

  boolean capeEnabled();

  void capeEnabled(boolean capeEnabled);

  boolean jacketEnabled();

  void jacketEnabled(boolean jacketEnabled);

  boolean leftSleeveEnabled();

  void leftSleeveEnabled(boolean leftSleeveEnabled);

  boolean rightSleeveEnabled();

  void rightSleeveEnabled(boolean rightSleeveEnabled);

  boolean leftPantsLegEnabled();

  void leftPantsLegEnabled(boolean leftPantsLegEnabled);

  boolean rightPantsLegEnabled();

  void rightPantsLegEnabled(boolean rightPantsLegEnabled);

  boolean hatEnabled();

  void hatEnabled(boolean hatEnabled);

  @Useless
    // What is this
  boolean unusedEnabled();

  @Useless
    // What is this
  void unusedEnabled(boolean unusedEnabled);

  @Useless
  Hand mainHand();

  @Useless
  void mainHand(@NotNull Hand mainHand);

  NBTCompound leftShoulderData();

  void leftShoulderData(NBTCompound leftShoulderData);

  default void leftShoulderData(@NotNull Consumer<NBTCompound> leftShoulderDataConsumer) {
    checkNotNull(leftShoulderDataConsumer, "leftShoulderDataConsumer");

    final NBTCompound leftShoulderData = leftShoulderData();
    leftShoulderDataConsumer.accept(leftShoulderData);
    leftShoulderData(leftShoulderData);
  }

  NBTCompound rightShoulderData();

  void rightShoulderData(NBTCompound rightShoulderData);

  default void rightShoulderData(@NotNull Consumer<NBTCompound> rightShoulderDataConsumer) {
    checkNotNull(rightShoulderDataConsumer, "rightShoulderDataConsumer");

    final NBTCompound rightShoulderData = rightShoulderData();
    rightShoulderDataConsumer.accept(rightShoulderData);
    rightShoulderData(rightShoulderData);
  }

  enum Hand {
    LEFT,
    RIGHT
  }

  enum Ping {
    NO_BARS(-1),
    ONE_BAR(1200),
    TWO_BARS(800),
    THREE_BARS(500),
    FOUR_BARS(200),
    FIVE_BARS(10);

    private final int ping;

    Ping(int ping) {
      this.ping = ping;
    }

    public int getPing() {
      return ping;
    }
  }
}
