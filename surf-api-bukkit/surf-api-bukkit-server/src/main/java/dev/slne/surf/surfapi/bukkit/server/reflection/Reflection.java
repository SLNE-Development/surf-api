package dev.slne.surf.surfapi.bukkit.server.reflection;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import dev.slne.surf.surfapi.core.server.impl.reflection.SurfReflectionImpl;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public final class Reflection {

  public static final ServerStatsCounterProxy SERVER_STATS_COUNTER_PROXY;
  public static final SynchedEntityDataProxy SYNCHED_ENTITY_DATA_PROXY;
  public static final ItemProxy ITEM_PROXY;

  static {
    final ReflectionRemapper remapper = ReflectionRemapper.forReobfMappingsInPaperJar();
    final ReflectionProxyFactory proxyFactory = ReflectionProxyFactory.create(remapper,
        Reflection.class.getClassLoader());
    final SurfReflection surfReflection = SurfReflectionImpl.INSTANCE;

    SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy(ServerStatsCounterProxy.class);
    SYNCHED_ENTITY_DATA_PROXY = proxyFactory.reflectionProxy(SynchedEntityDataProxy.class);
    ITEM_PROXY = surfReflection.createProxy(ItemProxy.class);

    // gc the remapper
    System.gc();
  }
}
