package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketPlayer;
import dev.slne.surf.surfapi.core.server.util.PlayerSkinFetcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.*;

public final class PacketPlayerImpl extends PacketLivingEntityImpl<PacketPlayer> implements PacketPlayer { // TODO: testing bc I think it`s broken

    private final UserProfile npcProfile;
    private boolean listed = false;
    private Ping ping = Ping.NO_BARS;
    private GameMode gameMode = GameMode.SURVIVAL;

    public PacketPlayerImpl(UUID uuid) {
        super(uuid, EntityTypes.PLAYER);

        this.npcProfile = new UserProfile(uuid, LegacyComponentSerializer.legacySection().serialize(displayName().orElse(Component.text("Player"))));
    }

    @Override
    public List<TextureProperty> skinProperties() {
        return npcProfile.getTextureProperties();
    }

    @Override
    public void skinProperties(@NotNull List<TextureProperty> skinProperties) {
        checkNotNull(skinProperties, "skinProperties");

        npcProfile.setTextureProperties(skinProperties);
    }

    @Override
    public @NotNull CompletableFuture<Void> skinProperties(@NotNull UUID playerUuid, boolean respawn) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        PlayerSkinFetcher.INSTANCE.fetchSkin(playerUuid, textureProperties -> {
            skinProperties(textureProperties);
            if (respawn) {
                respawn();
            }
            future.complete(null);
        });

        return future;
    }

    @Override
    public float additionalHearts() {
        return get(ADDITIONAL_HEARTS_INDEX, 0.0f);
    }

    @Override
    public void additionalHearts(float additionalHearts) {
        set(ADDITIONAL_HEARTS_INDEX, additionalHearts);
        afterSet();
    }

    @Override
    public boolean showOnTabList() {
        return listed;
    }

    @Override
    public void showOnTabList(boolean showOnTabList) {
        listed = showOnTabList;

        if (spawned) {
            sendPacketToAllViewers(version -> infoPacket(version, Action.UPDATE_LISTED, listed ? WrapperPlayServerPlayerInfo.Action.ADD_PLAYER : WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER));
        }
    }

    @Override
    public Ping ping() {
        return ping;
    }

    @Override
    public void ping(@NotNull Ping ping) {
        this.ping = checkNotNull(ping, "Ping cannot be null");

        if (spawned) {
            sendPacketToAllViewers(version -> infoPacket(version, Action.UPDATE_LATENCY, WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY));
        }
    }

    @Override
    public GameMode gameMode() {
        return gameMode;
    }

    @Override
    public void gameMode(@NotNull GameMode gameMode) {
        this.gameMode = checkNotNull(gameMode, "GameMode cannot be null");

        if (spawned) {
            sendPacketToAllViewers(version -> infoPacket(version, Action.UPDATE_GAME_MODE, WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE));
        }
    }

    @Override
    public void displayName(@Nullable Component displayName) {
        super.displayName(displayName); // TODO: needed?

        npcProfile.setName(PlainTextComponentSerializer.plainText().serialize(displayName != null ? displayName : Component.text("Player")));
        if (spawned) {
            sendPacketToAllViewers(version -> infoPacket(version, Action.UPDATE_DISPLAY_NAME, WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME));
        }
    }

    @Override
    public int score() {
        return get(SCORE_INDEX, 0);
    }

    @Override
    public void score(int score) {
        set(SCORE_INDEX, score);
        afterSet();
    }

    @Override
    public boolean capeEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, CAPE_ENABLED_BIT);
    }

    @Override
    public void capeEnabled(boolean capeEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, CAPE_ENABLED_BIT, capeEnabled);
        afterSet();
    }

    @Override
    public boolean jacketEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, JACKET_ENABLED_BIT);
    }

    @Override
    public void jacketEnabled(boolean jacketEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, JACKET_ENABLED_BIT, jacketEnabled);
        afterSet();
    }

    @Override
    public boolean leftSleeveEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, LEFT_SLEEVE_ENABLED_BIT);
    }

    @Override
    public void leftSleeveEnabled(boolean leftSleeveEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, LEFT_SLEEVE_ENABLED_BIT, leftSleeveEnabled);
        afterSet();
    }

    @Override
    public boolean rightSleeveEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, RIGHT_SLEEVE_ENABLED_BIT);
    }

    @Override
    public void rightSleeveEnabled(boolean rightSleeveEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, RIGHT_SLEEVE_ENABLED_BIT, rightSleeveEnabled);
        afterSet();
    }

    @Override
    public boolean leftPantsLegEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, LEFT_PANTS_LEG_ENABLED_BIT);
    }

    @Override
    public void leftPantsLegEnabled(boolean leftPantsLegEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, LEFT_PANTS_LEG_ENABLED_BIT, leftPantsLegEnabled);
        afterSet();
    }

    @Override
    public boolean rightPantsLegEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, RIGHT_PANTS_LEG_ENABLED_BIT);
    }

    @Override
    public void rightPantsLegEnabled(boolean rightPantsLegEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, RIGHT_PANTS_LEG_ENABLED_BIT, rightPantsLegEnabled);
        afterSet();
    }

    @Override
    public boolean hatEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, HAT_ENABLED_BIT);
    }

    @Override
    public void hatEnabled(boolean hatEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, HAT_ENABLED_BIT, hatEnabled);
        afterSet();
    }

    @Override
    public boolean unusedEnabled() {
        return getMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, UNUSED_ENABLED_BIT);
    }

    @Override
    public void unusedEnabled(boolean unusedEnabled) {
        setMaskBit(DISPLAY_SKIN_PART_BIT_MASK_INDEX, UNUSED_ENABLED_BIT, unusedEnabled);
        afterSet();
    }

    @Override
    public Hand mainHand() {
        return Hand.values()[get(MAIN_HAND_INDEX, (byte) Hand.RIGHT.ordinal())];
    }

    @Override
    public void mainHand(@NotNull Hand mainHand) {
        set(MAIN_HAND_INDEX, mainHand.ordinal());
        afterSet();
    }

    @Override
    public NBTCompound leftShoulderData() {
        return get(LEFT_SHOULDER_DATA_INDEX, new NBTCompound());
    }

    @Override
    public void leftShoulderData(NBTCompound leftShoulderData) {
        set(LEFT_SHOULDER_DATA_INDEX, EntityDataTypes.NBT, leftShoulderData);
        afterSet();
    }

    @Override
    public NBTCompound rightShoulderData() {
        return get(RIGHT_SHOULDER_DATA_INDEX, new NBTCompound());
    }

    @Override
    public void rightShoulderData(NBTCompound rightShoulderData) {
        set(RIGHT_SHOULDER_DATA_INDEX, EntityDataTypes.NBT, rightShoulderData);
        afterSet();
    }

    @Override
    protected void spawn(UUID uuid) {
        sendPacketsToViewer(uuid, this::infoAllPacket, this::spawnPlayerPacket);
    }

    @Override
    protected void despawn(UUID uuid) {
        sendPacketToViewer(uuid, version -> {
            if (version.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
                return new WrapperPlayServerPlayerInfoRemove(uuid());
            } else {
                return new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER);
            }
        });
        super.despawn(uuid);
    }

    private PacketWrapper<?> spawnPlayerPacket(ClientVersion version) {
        assert location != null;
        if (version.isNewerThanOrEquals(ClientVersion.V_1_20_2)) {
            return spawnPacket();
        } else {
            return new WrapperPlayServerSpawnPlayer(entityId(), uuid(), location, this.entityData(version));
        }
    }

    private PacketWrapper<?> infoPacket(ClientVersion version, Action modernAction, WrapperPlayServerPlayerInfo.Action legacyAction) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            return new WrapperPlayServerPlayerInfoUpdate(
                    modernAction,
                    getModernPlayerInfo()
            );
        } else {
            return new WrapperPlayServerPlayerInfo(
                    legacyAction,
                    getLegacyPlayerData()
            );
        }
    }

    private PacketWrapper<?> infoAllPacket(ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            return new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.allOf(Action.class),
                    getModernPlayerInfo()
            );
        } else {
            return new WrapperPlayServerPlayerInfo(
                    listed ? WrapperPlayServerPlayerInfo.Action.ADD_PLAYER : WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                    getLegacyPlayerData()
            );
        }
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo getModernPlayerInfo() {
        return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                npcProfile,
                listed,
                ping.getPing(),
                gameMode,
                displayName().orElse(null),
                null
        );
    }

    private WrapperPlayServerPlayerInfo.PlayerData getLegacyPlayerData() {
        return new WrapperPlayServerPlayerInfo.PlayerData(
                displayName().orElse(null),
                npcProfile,
                gameMode,
                ping.getPing()
        );
    }
}
