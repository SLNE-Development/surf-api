package dev.slne.surf.surfapi.bukkit.api.packet.listener;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitPacketListenerApi {

  static SurfBukkitPacketListenerApi get() {
    return SurfBukkitPacketApi.get().getPacketListenerApi();
  }

  void registerListeners(final PacketListener listener);

  void unregisterListeners(final PacketListener listener);
}
