package dev.slne.surf.surfapi.bukkit.server.impl.packet.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
public final class SurfBukkitPacketListenerApiImpl implements SurfBukkitPacketListenerApi {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

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
        logger.atSevere()
            .withStackTrace(StackSize.MEDIUM)
            .log("Failed to register listener method '%s.%s' due to illegal access",
                method.getDeclaringClass().getPackageName(), method.getName());
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

  public Packet<?> handleClientboundPacket(final Packet<?> packet,
      final net.minecraft.server.level.ServerPlayer serverPlayer) {
    final ObjectList<ListenerMethod> methods = clientboundListenerMethods.get(packet.getClass());
    Packet<?> result = packet;

    if (methods == null) {
      return packet;
    }
    try {
      for (final ListenerMethod listenerMethod : methods) {
        result = callListener(listenerMethod, serverPlayer, packet);

        if (result == null) {
          break;
        }
      }
    } catch (final Throwable t) {
      logger.atSevere()
          .withCause(t)
          .log("Failed to handle clientbound packet");
    }

    return result;
  }

  public Packet<?> handleServerboundPacket(final Packet<?> packet,
      final net.minecraft.server.level.ServerPlayer serverPlayer) {
    final ObjectList<ListenerMethod> methods = serverboundListenerMethods.get(packet.getClass());
    Packet<?> result = packet;

    if (methods == null) {
      return packet;
    }
    try {
      for (final ListenerMethod listenerMethod : methods) {
        result = callListener(listenerMethod, serverPlayer, packet);

        if (result == null) {
          break;
        }
      }
    } catch (Throwable t) {
      logger.atSevere()
          .withCause(t)
          .log("Failed to handle serverbound packet");
    }

    return result;
  }

  private @Nullable Packet<?> callListener(final ListenerMethod listenerMethod, ServerPlayer serverPlayer,
      Packet<?> packet) throws Throwable {
    if (listenerMethod.hasPlayerParameter) {
      final Object player = listenerMethod.hasServerPlayerParameter ? serverPlayer
          : serverPlayer.getBukkitEntity();
      final Object result = listenerMethod.methodHandle.invoke(listenerMethod.listener, packet,
          player);
      if (result instanceof final PacketListenerResult listenerResult && listenerResult == PacketListenerResult.CANCEL) {
        return null;
      } else if (result instanceof final Packet<?> newPacket) {
        return newPacket;
      }
    } else {
      final Object result = listenerMethod.methodHandle.invoke(listenerMethod.listener, packet);
      if (result instanceof final PacketListenerResult listenerResult && listenerResult == PacketListenerResult.CANCEL) {
        return null;
      } else if (result instanceof final Packet<?> newPacket) {
        return newPacket;
      }
    }

    return packet;
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
