package dev.slne.surf.surfapi.bukkit.server.reflection;

import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public final class Reflection {

  public static final EntityProxy ENTITY_PROXY;

  static {
    final ReflectionRemapper remapper = ReflectionRemapper.forReobfMappingsInPaperJar();
    final ReflectionProxyFactory proxyFactory = ReflectionProxyFactory.create(remapper,
        Reflection.class.getClassLoader());

    ENTITY_PROXY = proxyFactory.reflectionProxy(EntityProxy.class);

    // gc the remapper
    System.gc();
  }
}
