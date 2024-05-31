package dev.slne.surf.surfapi.bukkit.server.impl.nms;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.SurfBukkitNmsStatsBridgeImpl;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsBridgeImpl implements SurfBukkitNmsBridge {

  private final SurfBukkitNmsStatsBridgeImpl statsBridge;

  public SurfBukkitNmsBridgeImpl() {
    this.statsBridge = new SurfBukkitNmsStatsBridgeImpl();
  }

  @Override
  public SurfBukkitNmsStatsBridgeImpl getStatsBridge() {
    return statsBridge;
  }
}
