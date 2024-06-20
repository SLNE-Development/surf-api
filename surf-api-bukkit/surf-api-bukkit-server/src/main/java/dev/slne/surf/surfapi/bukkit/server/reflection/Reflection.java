package dev.slne.surf.surfapi.bukkit.server.reflection;

import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public final class Reflection {

  public static final ServerStatsCounterProxy SERVER_STATS_COUNTER_PROXY;
  public static final SynchedEntityDataProxy SYNCHED_ENTITY_DATA_PROXY;

  static {
    final ReflectionRemapper remapper = ReflectionRemapper.forReobfMappingsInPaperJar();
    final ReflectionProxyFactory proxyFactory = ReflectionProxyFactory.create(remapper,
        Reflection.class.getClassLoader());

    SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy(ServerStatsCounterProxy.class);
    SYNCHED_ENTITY_DATA_PROXY = proxyFactory.reflectionProxy(SynchedEntityDataProxy.class);

    // gc the remapper
    System.gc();
  }
}
