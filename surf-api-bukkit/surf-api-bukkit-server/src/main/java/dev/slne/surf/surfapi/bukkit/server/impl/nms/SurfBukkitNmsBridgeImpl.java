package dev.slne.surf.surfapi.bukkit.server.impl.nms;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsCommonBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsStatsBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.SurfBukkitNmsPacketBridgesImpl;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsBridgeImpl implements SurfBukkitNmsBridge {

  private final SurfBukkitNmsStatsBridgeImpl statsBridge;
  private final SurfBukkitNmsCommonBridgeImpl commonBridge;
  private final SurfBukkitNmsPacketBridgesImpl packetBridges;

  public SurfBukkitNmsBridgeImpl() {
    this.statsBridge = new SurfBukkitNmsStatsBridgeImpl();
    this.commonBridge = new SurfBukkitNmsCommonBridgeImpl();
    this.packetBridges = new SurfBukkitNmsPacketBridgesImpl();
  }

  @Override
  public SurfBukkitNmsStatsBridgeImpl getStatsBridge() {
    return statsBridge;
  }

  @Override
  public SurfBukkitNmsCommonBridgeImpl getCommonBridge() {
    return commonBridge;
  }

  @Override
  public SurfBukkitNmsPacketBridgesImpl getPacketBridges() {
    return packetBridges;
  }
}
