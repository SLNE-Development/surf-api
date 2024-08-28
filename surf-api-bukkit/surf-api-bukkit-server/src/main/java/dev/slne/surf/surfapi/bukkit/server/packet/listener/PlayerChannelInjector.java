package dev.slne.surf.surfapi.bukkit.server.packet.listener;

import com.google.common.flogger.FluentLogger;
import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.SurfBukkitNmsBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.PacketRegistry;
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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerChannelInjector implements Listener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  public static final PlayerChannelInjector INSTANCE = new PlayerChannelInjector();

  private static final Key CHANNEL_KEY = Key.key("surf-api", "packet-listener");
  private static final String CHANNEL_NAME = "surf_api_packet_listener";

  private final Object2ObjectMap<UUID, ServerPlayer> playerInjectorCache;
  private final ObjectSet<Channel> injectedChannels;

  private PlayerChannelInjector() {
    this.playerInjectorCache = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    this.injectedChannels = ObjectSets.synchronize(new ObjectOpenHashSet<>());
  }

  public void register() {
    ChannelInitializeListenerHolder.addListener(CHANNEL_KEY,
        this::injectChannel);
  }

  private PacketHandler injectChannel(Channel channel) {
    final PacketHandler channelHandler = new PacketHandler();

    channel.eventLoop().submit(() -> {
      if (injectedChannels.add(channel)) {
        final ChannelPipeline pipeline = channel.pipeline();

        if (pipeline.get(CHANNEL_NAME) != null) {
          return;
        }

        pipeline.addBefore("packet_handler", CHANNEL_NAME, channelHandler);
      }
    });

    return channelHandler;
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

      return;
    }

    injectChannel(channel).player = player;
  }


  /**
   * Handles packet processing for both clientbound and serverbound packets in a Netty channel. This
   * class extends {@link ChannelDuplexHandler} to intercept and process packets at both the read
   * and write stages. It associates a {@link ServerPlayer} with the channel, and utilizes packet
   * listeners to handle and potentially modify packets.
   */
  private final class PacketHandler extends ChannelDuplexHandler {

    private volatile ServerPlayer player;

    /**
     * Called when the channel is unregistered from the event loop. This method ensures that the
     * superclass's unregistered method is also called.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ChannelHandler} belongs to
     * @throws Exception if an error occurs while unregistering the channel
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
      injectedChannels.remove(ctx.channel());
      super.channelUnregistered(ctx);
    }

    /**
     * Intercepts the write operation in the Netty pipeline. This method handles the clientbound
     * packets, potentially modifying or canceling them based on custom logic.
     *
     * @param ctx     the {@link ChannelHandlerContext} which this {@link ChannelHandler} belongs
     *                to
     * @param msg     the message to write
     * @param promise the {@link ChannelPromise} to be notified once the operation completes
     * @throws Exception if an error occurs during the write operation
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
        throws Exception { // server -> client

      if (!(msg instanceof Packet<?> packet)) { // not our packet
        super.write(ctx, msg, promise);
        return;
      }

      // first we set the player if it isn't set yet
      if (player == null
          && msg instanceof ClientboundGameProfilePacket clientboundGameProfilePacket) {
        final UUID uuid = clientboundGameProfilePacket.gameProfile().getId();
        @Nullable final ServerPlayer player = playerInjectorCache.remove(uuid);

        if (player != null) {
          this.player = player;
        }
      }

      boolean cancelled = false;

      try {
        // first we try to handle the packet with the nms packet listener
        packet = getPacketListenerApi().handleClientboundPacket(packet, player);

        if (packet == null) {
          // no need to handle the packet further
          cancelled = true;
        } else {
          // then we try to handle the packet with the api packet listener
          msg = handleClientboundPacketFromBridge(packet);
          cancelled = (msg == null);
        }
      } catch (OutOfMemoryError error) {
        throw error;
      } catch (Throwable t) {
        logger.atSevere()
            .withCause(t)
            .log("Failed to handle clientbound packet");
        super.write(ctx, msg, promise);
      }

      if (!cancelled) {
        super.write(ctx, msg, promise);
      }
    }

    /**
     * Intercepts the read operation in the Netty pipeline. This method handles the serverbound
     * packets, potentially modifying or canceling them based on custom logic.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ChannelHandler} belongs to
     * @param msg the message to read
     * @throws Exception if an error occurs during the read operation
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
        throws Exception { // client -> server
      if (!(msg instanceof Packet<?> packet)) { // not our packet
        super.channelRead(ctx, msg);
        return;
      }

      boolean cancelled = false;

      try {
        // first we try to handle the packet with the nms packet listener
        packet = getPacketListenerApi().handleServerboundPacket(packet, player);

        if (packet == null) {
          // no need to handle the packet further
          cancelled = true;
        } else {
          // then we try to handle the packet with the api packet listener
          msg = handleServerboundPacketFromBridge(packet);
          cancelled = (msg == null);
        }
      } catch (OutOfMemoryError error) {
        throw error;
      } catch (Throwable t) {
        logger.atSevere()
            .withCause(t)
            .log("Failed to handle serverbound packet");
        super.channelRead(ctx, msg);
      }

      if (!cancelled) {
        super.channelRead(ctx, msg);
      }
    }

    /**
     * Handles serverbound packets by bridging them to the appropriate handler and potentially
     * modifying them.
     *
     * @param packet the serverbound packet to handle
     * @return the modified packet, or {@code null} if the packet was canceled
     */
    private @Nullable Packet<?> handleServerboundPacketFromBridge(Packet<?> packet) {
      final NmsServerboundPacket apiPacket = PacketRegistry.createServerboundPacketOrNull(packet);

      if (apiPacket != null) { // we have an api packet wrapper for this packet
        final NmsServerboundPacket resultApi = getBridge().handleServerboundPacket(apiPacket,
            player.getBukkitEntity());

        if (resultApi != null) { // we may have a modified packet
          return NmsPacketImpl.getFromApi(resultApi).getNmsPacket();
        }
      } else {
        return packet; // no api packet wrapper, so we just return the original packet
      }

      return null; // the packet was canceled
    }

    /**
     * Handles clientbound packets by bridging them to the appropriate handler and potentially
     * modifying them.
     *
     * @param packet the clientbound packet to handle
     * @return the modified packet, or {@code null} if the packet was canceled
     */
    private @Nullable Packet<?> handleClientboundPacketFromBridge(Packet<?> packet) {
      final NmsClientboundPacket apiPacket = PacketRegistry.createClientboundPacketOrNull(packet);

      if (apiPacket != null) {
        final NmsClientboundPacket resultApi = getBridge().handleClientboundPacket(apiPacket,
            player.getBukkitEntity());

        if (resultApi != null) {
          return NmsPacketImpl.getFromApi(resultApi).getNmsPacket();
        }
      } else {
        return packet;
      }

      return null;
    }

    /**
     * Returns the SurfBukkit NMS bridge implementation.
     *
     * @return The SurfBukkit NMS bridge.
     */
    private @NotNull SurfBukkitNmsBridgeImpl getBridge() {
      return (SurfBukkitNmsBridgeImpl) SurfBukkitNmsBridge.get();
    }

    /**
     * Returns the SurfBukkit packet listener API implementation.
     *
     * @return The SurfBukkit packet listener API.
     */
    private @NotNull SurfBukkitPacketListenerApiImpl getPacketListenerApi() {
      return (SurfBukkitPacketListenerApiImpl) SurfBukkitPacketListenerApi.get();
    }
  }
}
