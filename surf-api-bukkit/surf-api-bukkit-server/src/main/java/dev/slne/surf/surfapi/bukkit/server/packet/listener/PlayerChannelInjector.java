package dev.slne.surf.surfapi.bukkit.server.packet.listener;

import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.server.impl.packet.listener.SurfBukkitPacketListenerApiImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;

public class PlayerChannelInjector implements Listener {

  public static final PlayerChannelInjector INSTANCE = new PlayerChannelInjector();

  private static final ComponentLogger LOGGER = ComponentLogger.logger("PlayerChannelInjector");
  private static final Key CHANNEL_KEY = Key.key("surf-api", "packet-listener");
  private static final String CHANNEL_NAME = "surf_api_packet_listener";

  private final Object2ObjectMap<UUID, ServerPlayer> playerInjectorCache;

  private PlayerChannelInjector() {
    this.playerInjectorCache = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  }

  public void register() {
    ChannelInitializeListenerHolder.addListener(CHANNEL_KEY, channel -> {
      final ChannelPipeline pipeline = channel.pipeline();

      if (pipeline.get(CHANNEL_NAME) != null) {
        return;
      }

      pipeline.addBefore("packet_handler", CHANNEL_NAME, new PacketHandler());
    });
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerLogin(PlayerLoginEvent event) {
    final ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
    playerInjectorCache.put(player.getUUID(), player);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    final ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();

    final Channel channel = player.connection.connection.channel;
    final ChannelHandler channelHandler = channel.pipeline().get(CHANNEL_NAME);

    if (channelHandler != null) {
      if (channelHandler instanceof PacketHandler packetHandler) {
        packetHandler.player = player; // Just in case the player is not set yet
        playerInjectorCache.remove(player.getUUID());
      }
    }
  }


  private final class PacketHandler extends ChannelDuplexHandler {

    private volatile ServerPlayer player;

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

      super.channelUnregistered(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
        throws Exception {

      if (player == null
          && msg instanceof ClientboundGameProfilePacket clientboundGameProfilePacket) {
        final UUID uuid = clientboundGameProfilePacket.getGameProfile().getId();
        @Nullable final ServerPlayer player = playerInjectorCache.get(uuid);

        if (player != null) {
          this.player = player;
        }
      }

      if (!(msg instanceof Packet<?> packet)) {
        super.write(ctx, msg, promise);
        return;
      }

      boolean cancelled = false;

      try {
        final PacketListenerResult result = ((SurfBukkitPacketListenerApiImpl) SurfBukkitPacketListenerApi.get()).handleClientboundPacket(
            packet, player);

        if (result == PacketListenerResult.CANCEL) {
          cancelled = true;
        }
      } catch (Throwable t) {
        LOGGER.error("Failed to handle clientbound packet", t);
        throw t;
      } finally {
        if (!cancelled) {
          super.write(ctx, msg, promise);
        }
      }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (!(msg instanceof Packet<?> packet)) {
        super.channelRead(ctx, msg);
        return;
      }

      boolean cancelled = false;

      try {
        final PacketListenerResult result = ((SurfBukkitPacketListenerApiImpl) SurfBukkitPacketListenerApi.get()).handleServerboundPacket(
            packet, player);

        if (result == PacketListenerResult.CANCEL) {
          cancelled = true;
        }
      } catch (Throwable t) {
        LOGGER.error("Failed to handle serverbound packet", t);
        throw t;
      } finally {
        if (!cancelled) {
          super.channelRead(ctx, msg);
        }
      }
    }
  }
}
