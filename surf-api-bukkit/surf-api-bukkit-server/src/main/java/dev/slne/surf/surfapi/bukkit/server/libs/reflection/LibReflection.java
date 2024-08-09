package dev.slne.surf.surfapi.bukkit.server.libs.reflection;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import dev.slne.surf.surfapi.core.server.impl.reflection.SurfReflectionImpl;

public final class LibReflection {

  public static final PaperPluginClassLoaderProxy PAPER_PLUGIN_CLASS_LOADER_PROXY;

  static {
    final SurfReflection reflection = SurfReflectionImpl.INSTANCE;

    PAPER_PLUGIN_CLASS_LOADER_PROXY = reflection.createProxy(PaperPluginClassLoaderProxy.class);
  }
}
