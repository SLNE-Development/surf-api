package dev.slne.surf.surfapi.bukkit.server.impl.packet.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

@ParametersAreNonnullByDefault
public final class SurfBukkitPacketListenerApiImpl implements SurfBukkitPacketListenerApi {

  // @formatter:off
  private static final ComponentLogger LOGGER = ComponentLogger.logger("SurfBukkitPacketListenerApi");
  // @formatter:on

  private final Object2ObjectMap<Class<?>, ObjectList<ListenerMethod>> clientboundListenerMethods;
  private final Object2ObjectMap<Class<?>, ObjectList<ListenerMethod>> serverboundListenerMethods;

  public SurfBukkitPacketListenerApiImpl() {
    // @formatter:off
    this.clientboundListenerMethods  = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    this.serverboundListenerMethods  = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    // @formatter:on
  }

  @Override
  public void registerListeners(final PacketListener listener) {
    checkNotNull(listener, "listener");

    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    for (final Method method : listener.getClass().getMethods()) {
      try {
        if (method.isAnnotationPresent(ClientboundListener.class)) {
          registerListenerMethod(listener, lookup, method, clientboundListenerMethods);
        } else if (method.isAnnotationPresent(ServerboundListener.class)) {
          registerListenerMethod(listener, lookup, method, serverboundListenerMethods);
        }
      } catch (final IllegalAccessException e) {
        LOGGER.error("Failed to register listener method: {}", method.getName(), e);
      }
    }
  }

  private void registerListenerMethod(
      PacketListener listener,
      Lookup lookup,
      Method method,
      Object2ObjectMap<Class<?>, ObjectList<ListenerMethod>> clientboundListenerMethods
  ) throws IllegalAccessException {
    final MethodHandle methodHandle = lookup.unreflect(method);
    final boolean hasPlayerParameter = method.getParameterCount() == 2;
    final boolean hasServerPlayerParameter =
        hasPlayerParameter && method.getParameterTypes()[1].isAssignableFrom(
            ServerPlayer.class);
    clientboundListenerMethods
        .computeIfAbsent(method.getParameterTypes()[0], k -> new ObjectArrayList<>())
        .add(new ListenerMethod(listener, methodHandle, hasPlayerParameter,
            hasServerPlayerParameter));
  }

  @Override
  public void unregisterListeners(final PacketListener listener) {
    checkNotNull(listener, "listener");

    for (final List<ListenerMethod> methods : clientboundListenerMethods.values()) {
      methods.removeIf(listenerMethod -> listenerMethod.listener.equals(listener));
    }
    for (final List<ListenerMethod> methods : serverboundListenerMethods.values()) {
      methods.removeIf(listenerMethod -> listenerMethod.listener.equals(listener));
    }
  }

  public PacketListenerResult handleClientboundPacket(final Packet<?> packet,
      final net.minecraft.server.level.ServerPlayer serverPlayer) {
    final ObjectList<ListenerMethod> methods = clientboundListenerMethods.get(packet.getClass());
    final ObjectSet<PacketListenerResult> results = new ObjectArraySet<>();

    if (methods == null) {
      return PacketListenerResult.CONTINUE;
    }
    try {
      for (final ListenerMethod listenerMethod : methods) {
        callListener(listenerMethod, serverPlayer, packet, results);
      }
    } catch (final Throwable t) {
      LOGGER.error("Failed to handle clientbound packet", t);
    }

    return reduceResults(results);
  }

  public PacketListenerResult handleServerboundPacket(final Packet<?> packet,
      final net.minecraft.server.level.ServerPlayer serverPlayer) {
    final ObjectList<ListenerMethod> methods = serverboundListenerMethods.get(packet.getClass());
    final ObjectSet<PacketListenerResult> results = new ObjectArraySet<>();

    if (methods == null) {
      return PacketListenerResult.CONTINUE;
    }
    try {
      for (final ListenerMethod listenerMethod : methods) {
        callListener(listenerMethod, serverPlayer, packet, results);
      }
    } catch (Throwable t) {
      LOGGER.error("Failed to handle serverbound packet", t);
    }

    return reduceResults(results);
  }

  private void callListener(final ListenerMethod listenerMethod, ServerPlayer serverPlayer,
      Packet<?> packet, ObjectSet<PacketListenerResult> results) throws Throwable {
    if (listenerMethod.hasPlayerParameter) {
      final Object player = listenerMethod.hasServerPlayerParameter ? serverPlayer
          : serverPlayer.getBukkitEntity();
      final Object result = listenerMethod.methodHandle.invoke(listenerMethod.listener, packet,
          player);
      if (result instanceof final PacketListenerResult listenerResult) {
        results.add(listenerResult);
      }
    } else {
      final Object result = listenerMethod.methodHandle.invoke(listenerMethod.listener, packet);
      if (result instanceof final PacketListenerResult listenerResult) {
        results.add(listenerResult);
      }
    }
  }

  private PacketListenerResult reduceResults(ObjectSet<PacketListenerResult> results) {
    return results.stream().reduce((listenerResult, listenerResult2) -> {
      if (listenerResult == PacketListenerResult.CANCEL
          || listenerResult2 == PacketListenerResult.CANCEL) {
        return PacketListenerResult.CANCEL;
      }
      return PacketListenerResult.CONTINUE;
    }).orElse(PacketListenerResult.CONTINUE);
  }

  private record ListenerMethod(PacketListener listener, MethodHandle methodHandle,
                                boolean hasPlayerParameter, boolean hasServerPlayerParameter) {

  }
}
