package dev.slne.surf.surfapi.bukkit.server.libs.reflection;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Field;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Field.Type;
import dev.slne.surf.surfapi.core.api.reflection.annontation.SurfProxy;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import java.net.URLClassLoader;

@SurfProxy(PaperPluginClassLoader.class)
public interface PaperPluginClassLoaderProxy {

  @Field(name = "libraryLoader", type = Type.SETTER, overrideFinal = true)
  void setLibraryLoader(PaperPluginClassLoader instance, URLClassLoader libraryLoader);

  @Field(name = "libraryLoader", type = Type.GETTER)
  URLClassLoader getLibraryLoader(PaperPluginClassLoader instance);
}
