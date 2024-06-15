package dev.slne.surf.surfapi.bukkit.server.impl.nms;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsCommonBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsStatsBridgeImpl;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsBridgeImpl implements SurfBukkitNmsBridge {

  private final SurfBukkitNmsStatsBridgeImpl statsBridge;
  private final SurfBukkitNmsCommonBridgeImpl commonBridge;

  public SurfBukkitNmsBridgeImpl() {
    this.statsBridge = new SurfBukkitNmsStatsBridgeImpl();
    this.commonBridge = new SurfBukkitNmsCommonBridgeImpl();
  }

  @Override
  public SurfBukkitNmsStatsBridgeImpl getStatsBridge() {
    return statsBridge;
  }

  @Override
  public SurfBukkitNmsCommonBridgeImpl getCommonBridge() {
    return commonBridge;
  }
}
