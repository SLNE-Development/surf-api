package dev.slne.surf.surfapi.bukkit.server.reflection;

import net.minecraft.stats.ServerStatsCounter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@Proxies(ServerStatsCounter.class)
public interface ServerStatsCounterProxy {

  @MethodName("toJson")
  String toJson(ServerStatsCounter statsCounter);
}
