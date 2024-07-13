package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import com.mojang.math.Transformation;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SurfBukkitNmsSpawnPackets;
import dev.slne.surf.surfapi.bukkit.server.annotation.VerifyOnMinecraftUpdate;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.FinePosition;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsSpawnPacketsImpl implements SurfBukkitNmsSpawnPackets, NmsUtil {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public PacketOperation despawn(IntList entityIds) {
    checkNotNull(entityIds, "entityIds");

    return PacketOperationImpl.simple(player -> new ClientboundRemoveEntitiesPacket(entityIds));
  }

  @Override
  public PacketOperationImpl despawn(int... entityIds) {
    checkNotNull(entityIds, "entityIds");

    return PacketOperationImpl.simple(player -> new ClientboundRemoveEntitiesPacket(entityIds));
  }

  @Override
  public PacketOperation spawnItemDisplay(int entityId, FinePosition position,
      ItemDisplaySettings settings) {
    checkNotNull(position, "position");
    checkNotNull(settings, "settings");

    return PacketOperationImpl.complex((player, packets) -> {
      final ServerPlayer serverPlayer = toNms(player);

      final ItemDisplay display = new ItemDisplay(EntityType.ITEM_DISPLAY, serverPlayer.level());
      display.setId(entityId);

      setPosition(display, position);
      applySettings(display, settings);

      display.setItemStack(toNms(settings.getItemStack()));
      display.setItemTransform(toNms(settings.getItemDisplayTransform()));

      packets.add(new ClientboundAddEntityPacket(display, 0, display.blockPosition()));
      packets.add(new ClientboundSetEntityDataPacket(entityId,
          Reflection.SYNCHED_ENTITY_DATA_PROXY.packAll(display.getEntityData())));
      return packets;
    });
  }

  @Override
  public PacketOperation spawnTextDisplay(int entityId, FinePosition position,
      TextDisplaySettings settings) {
    checkNotNull(position, "position");
    checkNotNull(settings, "settings");

    return PacketOperationImpl.complex((player, packets) -> {
      final ServerPlayer serverPlayer = toNms(player);

      final TextDisplay display = new TextDisplay(EntityType.TEXT_DISPLAY, serverPlayer.level());
      display.setId(entityId);

      setPosition(display, position);
      applySettings(display, settings);
      display.setText(PaperAdventure.asVanilla(settings.getText()));
      display.getEntityData().set(TextDisplay.DATA_LINE_WIDTH_ID, settings.getLineWidth());
      display.getEntityData()
          .set(TextDisplay.DATA_BACKGROUND_COLOR_ID, settings.getBackgroundColor().value());

      switch (settings.getTextAlignment()) {
        case CENTER -> {
          setFlag(display, TextDisplay.FLAG_ALIGN_LEFT, false);
          setFlag(display, TextDisplay.FLAG_ALIGN_RIGHT, false);
        }
        case LEFT -> {
          setFlag(display, TextDisplay.FLAG_ALIGN_LEFT, true);
          setFlag(display, TextDisplay.FLAG_ALIGN_RIGHT, false);
        }
        case RIGHT -> {
          setFlag(display, TextDisplay.FLAG_ALIGN_LEFT, false);
          setFlag(display, TextDisplay.FLAG_ALIGN_RIGHT, true);
        }
      }

      packets.add(new ClientboundAddEntityPacket(display, 0, display.blockPosition()));
      packets.add(new ClientboundSetEntityDataPacket(entityId,
          Reflection.SYNCHED_ENTITY_DATA_PROXY.packAll(display.getEntityData())));
      return packets;
    });
  }

  @Override
  public PacketOperation updateSign(int entityId, BlockPosition position,
      SignBlockUpdateSettings settings) {

    return PacketOperationImpl.complex((player, packets) -> {
      final CompoundTag nbt = new CompoundTag();
      final HolderLookup.Provider registryLookup = MinecraftServer.getServer().registryAccess();
      writeUpdateSignToTag(nbt, registryLookup, settings.getFrontText(), settings.getBackText());

      packets.add(new ClientboundBlockEntityDataPacket(toNms(position), BlockEntityType.SIGN, nbt));
      return packets;
    });
  }

  @VerifyOnMinecraftUpdate
  private void writeUpdateSignToTag(CompoundTag nbt, HolderLookup.Provider registryLookup, @NotNull
  SignBlockUpdateSettings.SignText frontText, @NotNull SignBlockUpdateSettings.SignText backText) {

    writeTextToTag(nbt, registryLookup, frontText, "front_text", true);
    writeTextToTag(nbt, registryLookup, backText, "back_text", false);
  }

  @VerifyOnMinecraftUpdate
  private void writeTextToTag(CompoundTag nbt, HolderLookup.Provider registryLookup,
      SignBlockUpdateSettings.SignText text, String tagText, boolean isFrontText) {
    final DynamicOps<Tag> nbtOps = registryLookup.createSerializationContext(NbtOps.INSTANCE);
    final DataResult<Tag> textTag = SignText.DIRECT_CODEC.encodeStart(nbtOps, toNms(text));

    textTag.resultOrPartial(s -> logFailedEncodeText(s, isFrontText))
        .ifPresent(textElement -> nbt.put(tagText, textElement));
  }

  private void logFailedEncodeText(String string, boolean front) {
    logger.atSevere()
        .withStackTrace(StackSize.MEDIUM)
        .atMostEvery(5, TimeUnit.SECONDS)
        .log("Failed to encode %s text: %s", front ? "front" : "back", string);
  }

  private Transformation getTransformation(DisplaySettings settings) {
    return new Transformation(
        toNms(settings.getTranslation()),
        toNms(settings.getLeftRotation()),
        toNms(settings.getScale()),
        toNms(settings.getRightRotation())
    );
  }

  private void applySettings(Display entity, DisplaySettings settings) {
    entity.setXRot(settings.getPitch());
    entity.setYRot(settings.getYaw());
    entity.setTransformation(getTransformation(settings));
    entity.setBillboardConstraints(toNms(settings.getBillboardConstraints()));
  }

  private void setPosition(Entity entity, FinePosition position) {
    entity.setPosRaw(position.x(), position.y(), position.z());
  }

  private void setFlag(TextDisplay entity, int flag, boolean set) {
    byte flagBits = entity.getFlags();

    if (set) {
      flagBits |= (byte) flag;
    } else {
      flagBits &= (byte) ~flag;
    }

    entity.setFlags(flagBits);
  }
}
