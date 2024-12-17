package dev.slne.surf.surfapi.bukkit.server.impl.nms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsItemBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsClientboundPacketListener;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsServerboundPacketListener;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsCommonBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsItemBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsNbtBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsStatsBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.SurfBukkitNmsPacketBridgesImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersAreNonnullByDefault
@Getter
@NoArgsConstructor
public final class SurfBukkitNmsBridgeImpl implements SurfBukkitNmsBridge {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Logger log = LoggerFactory.getLogger(SurfBukkitNmsBridgeImpl.class);

  // @formatter:off
  private final SurfBukkitNmsStatsBridgeImpl statsBridge = new SurfBukkitNmsStatsBridgeImpl();
  private final SurfBukkitNmsCommonBridgeImpl commonBridge = new SurfBukkitNmsCommonBridgeImpl();
  private final SurfBukkitNmsPacketBridgesImpl packetBridges = new SurfBukkitNmsPacketBridgesImpl();
  private final SurfBukkitNmsNbtBridgeImpl nbtBridge = new SurfBukkitNmsNbtBridgeImpl();

  private final Object2ObjectMap<Class<?>, ObjectSet<NmsServerboundPacketListener<?>>> serverboundPacketListeners = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  private final Object2ObjectMap<Class<?>, ObjectSet<NmsClientboundPacketListener<?>>> clientboundPacketListeners = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  // @formatter:on

  @Override
  public SurfBukkitNmsItemBridge getItemBridge() {
    return SurfBukkitNmsItemBridgeImpl.INSTANCE;
  }

  @Override
  public void registerServerboundPacketListener(NmsServerboundPacketListener<?> listener) {
    checkNotNull(listener, "listener");

    final Class<?> packetClass = listener.getPacketClass();
    final boolean added = serverboundPacketListeners.computeIfAbsent(packetClass,
        key -> ObjectSets.synchronize(new ObjectOpenHashSet<>())).add(listener);

    if (!added) {
      logger.atWarning()
          .withStackTrace(StackSize.MEDIUM)
          .log("Serverbound packet listener %s for packet class %s is already registered",
              listener, packetClass.getName());
    }
  }

  @Override
  public void unregisterServerboundPacketListener(NmsServerboundPacketListener<?> listener) {
    checkNotNull(listener, "listener");

    final Class<?> packetClass = listener.getPacketClass();
    final ObjectSet<NmsServerboundPacketListener<?>> listeners = serverboundPacketListeners.get(packetClass);

    if (listeners == null || !listeners.remove(listener)) {
      logger.atWarning()
          .withStackTrace(StackSize.MEDIUM)
          .log("Serverbound packet listener %s for packet class %s is not registered",
              listener, packetClass.getName());
    }
  }

  @Override
  public void registerClientboundPacketListener(NmsClientboundPacketListener<?> listener) {
    checkNotNull(listener, "listener");

    final Class<?> packetClass = listener.getPacketClass();
    final boolean added = clientboundPacketListeners.computeIfAbsent(packetClass,
        key -> ObjectSets.synchronize(new ObjectOpenHashSet<>())).add(listener);

    if (!added) {
      logger.atWarning()
          .withStackTrace(StackSize.MEDIUM)
          .log("Clientbound packet listener %s for packet class %s is already registered",
              listener, packetClass.getName());
    }
  }

  @Override
  public void unregisterClientboundPacketListener(NmsClientboundPacketListener<?> listener) {
    checkNotNull(listener, "listener");

    final Class<?> packetClass = listener.getPacketClass();
    final ObjectSet<NmsClientboundPacketListener<?>> listeners = clientboundPacketListeners.get(packetClass);

    if (listeners == null || !listeners.remove(listener)) {
      logger.atWarning()
          .withStackTrace(StackSize.MEDIUM)
          .log("Clientbound packet listener %s for packet class %s is not registered",
              listener, packetClass.getName());
    }
  }

  public<Packet extends NmsServerboundPacket> @Nullable Packet handleServerboundPacket(Packet packet, Player player) {
    Class<?> clazz = packet.packetClass;
    final ObjectSet<NmsServerboundPacketListener<?>> listeners = serverboundPacketListeners.get(
        clazz);

    if (listeners == null) {
      return packet;
    }

    Packet processedPacket = listeners.stream()
        .map(listener -> (NmsServerboundPacketListener<Packet>) listener)
        .map(listener -> listener.handleServerboundPacket(packet, player))
        .reduce(PacketListenerResult::combine)
        .filter(result -> result == PacketListenerResult.CANCEL)
        .<Packet>map(result -> null)
        .orElse(packet);
    return processedPacket;
  }

  public<Packet extends NmsClientboundPacket> @Nullable Packet handleClientboundPacket(Packet packet, Player player) {
    final ObjectSet<NmsClientboundPacketListener<?>> listeners = clientboundPacketListeners.get(
        NmsPacketImpl.getFromApi(packet).getNmsClass());

    if (listeners == null) {
      return packet;
    }

    return listeners.stream()
        .map(listener -> (NmsClientboundPacketListener<Packet>) listener)
        .map(listener -> listener.handleClientboundPacket(packet, player))
        .reduce(PacketListenerResult::combine)
        .filter(result -> result == PacketListenerResult.CANCEL)
        .<Packet>map(result -> null)
        .orElse(packet);
  }

}
