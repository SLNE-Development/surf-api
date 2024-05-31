package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsStatsBridge;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.entity.Player;

@ParametersAreNonnullByDefault
public final class SurfBukkitNmsStatsBridgeImpl implements SurfBukkitNmsStatsBridge, NmsUtil {

  @Override
  public String getPlayerStatsAsJson(Player player) {
    checkNotNull(player, "player");

    return Reflection.SERVER_STATS_COUNTER_PROXY.toJson(toNms(player).getStats());
  }

  @Override
  public void savePlayerStatsToFile(Player player) {
    checkNotNull(player, "player");

    toNms(player).getStats().save();
  }
}
