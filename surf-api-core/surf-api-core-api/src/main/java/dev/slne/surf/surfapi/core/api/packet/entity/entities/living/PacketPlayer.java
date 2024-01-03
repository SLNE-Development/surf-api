package dev.slne.surf.surfapi.core.api.packet.entity.entities.living;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

@CanBeSpawned
public interface PacketPlayer extends PacketLivingEntity<PacketPlayer>, Spawnable { // TODO: NPC functions

    int ADDITIONAL_HEARTS_INDEX = 15, SCORE_INDEX = 16, DISPLAY_SKIN_PART_BIT_MASK_INDEX = 17, MAIN_HAND_INDEX = 18,
            LEFT_SHOULDER_DATA_INDEX = 19, RIGHT_SHOULDER_DATA_INDEX = 20;

    byte CAPE_ENABLED_BIT = 0x01, JACKET_ENABLED_BIT = 0x02, LEFT_SLEEVE_ENABLED_BIT = 0x04,
            RIGHT_SLEEVE_ENABLED_BIT = 0x08, LEFT_PANTS_LEG_ENABLED_BIT = 0x10, RIGHT_PANTS_LEG_ENABLED_BIT = 0x20,
            HAT_ENABLED_BIT = 0x40, UNUSED_ENABLED_BIT = (byte) 0x80;

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
