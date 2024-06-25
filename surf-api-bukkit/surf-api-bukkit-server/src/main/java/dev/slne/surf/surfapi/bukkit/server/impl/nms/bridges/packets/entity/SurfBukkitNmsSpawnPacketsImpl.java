package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.mojang.math.Transformation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SurfBukkitNmsSpawnPackets;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.FinePosition;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsSpawnPacketsImpl implements SurfBukkitNmsSpawnPackets, NmsUtil {

  @Override
  public PacketOperationImpl despawn(int... entityIds) {
    return new PacketOperationImpl((player, packets) -> {
      packets.add(new ClientboundRemoveEntitiesPacket(entityIds));
      return packets;
    });
  }

  @Override
  public PacketOperationImpl spawnItemDisplay(int entityId, FinePosition position,
      Consumer<ItemDisplaySettings> settingsConsumer) {
    checkNotNull(position, "position");
    checkNotNull(settingsConsumer, "settingsConsumer");

    return new PacketOperationImpl((player, packets) -> {
      final ItemDisplaySettings settings = new ItemDisplaySettings();
      settingsConsumer.accept(settings);

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
      Consumer<TextDisplaySettings> settingsConsumer) {
    checkNotNull(position, "position");
    checkNotNull(settingsConsumer, "settingsConsumer");

    return new PacketOperationImpl((player, packets) -> {
      final TextDisplaySettings settings = new TextDisplaySettings();
      settingsConsumer.accept(settings);

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
