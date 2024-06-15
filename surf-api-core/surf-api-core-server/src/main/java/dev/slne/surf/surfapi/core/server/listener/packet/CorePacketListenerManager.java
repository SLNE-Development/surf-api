package dev.slne.surf.surfapi.core.server.listener.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.slne.surf.surfapi.core.server.listener.packet.chat.ChatPacketListener;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;

public final class CorePacketListenerManager {

  public static final CorePacketListenerManager INSTANCE = new CorePacketListenerManager();

  private final ObjectSet<PacketListenerCommon> listeners;

  private CorePacketListenerManager() {
    listeners = ObjectSets.synchronize(new ObjectArraySet<>(1));
  }

  public void registerListeners() {
    register(new ChatPacketListener(PacketListenerPriority.NORMAL));
  }

  public void unregisterListeners() {
    final EventManager eventManager = PacketEvents.getAPI().getEventManager();
    listeners.forEach(eventManager::unregisterListener);
  }

  private void register(@NotNull PacketListenerCommon listener) {
    listeners.add(listener);
    PacketEvents.getAPI().getEventManager().registerListener(listener);
  }
}
