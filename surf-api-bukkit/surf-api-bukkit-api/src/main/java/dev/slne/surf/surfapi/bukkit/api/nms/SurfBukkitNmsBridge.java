package dev.slne.surf.surfapi.bukkit.api.nms;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsCommonBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsNbtBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsStatsBridge;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsClientboundPacketListener;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsServerboundPacketListener;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsBridge {

  SurfBukkitNmsStatsBridge getStatsBridge();

  SurfBukkitNmsCommonBridge getCommonBridge();

  SurfBukkitNmsPacketBridges getPacketBridges();

  SurfBukkitNmsNbtBridge getNbtBridge();

  void registerServerboundPacketListener(NmsServerboundPacketListener<?> listener);

  void unregisterServerboundPacketListener(NmsServerboundPacketListener<?> listener);

  void registerClientboundPacketListener(NmsClientboundPacketListener<?> listener);

  void unregisterClientboundPacketListener(NmsClientboundPacketListener<?> listener);

  @NotNull
  static SurfBukkitNmsBridge get() {
    return SurfBukkitApi.get().getNmsBridge();
  }
}
