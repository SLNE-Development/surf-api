package dev.slne.surf.surfapi.bukkit.server.libs;

import com.google.common.flogger.FluentLogger;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.bukkit.server.libs.reflection.LibReflection;
import dev.slne.surf.surfapi.core.api.util.SurfUtil;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.ApiVersion;
import org.bukkit.craftbukkit.util.Commodore;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

public final class LibLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final PaperPluginClassLoader pluginClassLoader;

  public LibLoader(ClassLoader pluginClassLoader) {
    this.pluginClassLoader = (PaperPluginClassLoader) pluginClassLoader;
  }

  public void loadLibs() {
//    loadLib("IF-0.10.15.zip", "1.20");
  }

  @SuppressWarnings("UnstableApiUsage")
  private void loadLib(String jarName) {
    loadLib(jarName, BukkitMain.getInstance().getPluginMeta().getAPIVersion());
  }

  private void loadLib(String jarName, String apiVersion) {
    logger.atInfo()
        .log("Loading library %s", jarName);
    try {
      loadLib0(jarName, apiVersion);
    } catch (IOException e) {
      logger.atWarning()
          .withCause(e)
          .log("Failed to load library %s", jarName);
    }
  }

  private void loadLib0(String jarName, String apiVersion) throws IOException {
    final String fileNameWithoutExtension = getFileNameWithoutExtension(jarName);
    final File inputJar = loadTempFileFromResource(jarName);

    if (inputJar == null) {
      return;
    }

    final File outputJar = new File(inputJar.getParent(),
        fileNameWithoutExtension + "-remapped.jar");

    try (final JarFile inputJarFile = new JarFile(inputJar);
        final FileOutputStream outputFileStream = new FileOutputStream(outputJar);
        final JarOutputStream outputJarStream = new JarOutputStream(outputFileStream)) {

      final Enumeration<JarEntry> jarEntries = inputJarFile.entries();
      while (jarEntries.hasMoreElements()) {
        final JarEntry currentEntry = jarEntries.nextElement();

        try (final InputStream classInputStream = inputJarFile.getInputStream(currentEntry)) {
          if (currentEntry.getName().endsWith(".class")) {
            logger.atInfo()
                .log("Remapping class %s", currentEntry.getName());
            remapClass(currentEntry, classInputStream, outputJarStream, apiVersion);
            logger.atInfo()
                .log("Remapped class %s", currentEntry.getName());
          } else {
            copyRessource(currentEntry, classInputStream, outputJarStream);
          }
        }
      }
    }

    addJarToClassLoader(outputJar);
  }

  private void remapClass(
      JarEntry jarEntry,
      InputStream classInputStream,
      JarOutputStream outputJarStream,
      String apiVersion
  ) throws IOException {
    final byte[] clazz = IOUtils.toByteArray(classInputStream);
    final byte[] remappedClass = remapClass(jarEntry.getName(), clazz, apiVersion);

    final JarEntry remappedEntry = new JarEntry(jarEntry.getName());

    outputJarStream.putNextEntry(remappedEntry);
    outputJarStream.write(remappedClass);
    outputJarStream.closeEntry();
  }

  private void copyRessource(
      JarEntry jarEntry,
      InputStream classInputStream,
      JarOutputStream outputJarStream
  ) throws IOException {
    outputJarStream.putNextEntry(jarEntry);
    IOUtils.copy(classInputStream, outputJarStream);
    outputJarStream.closeEntry();
  }

  private byte[] remapClass(String jarName, byte[] clazz, String apiVersion) {
    return Commodore.convert(clazz, jarName, ApiVersion.getOrCreateVersion(apiVersion),
        ((CraftServer) Bukkit.getServer()).activeCompatibilities);
  }

  private @Nullable File loadTempFileFromResource(String fileName) throws IOException {
    final String fileNameWithoutExtension = getFileNameWithoutExtension(fileName);
    final File inputJar = File.createTempFile(fileNameWithoutExtension, ".jar");
    inputJar.deleteOnExit();

    try (final InputStream in = pluginClassLoader.getResourceAsStream(fileName);
        final FileOutputStream out = new FileOutputStream(inputJar)) {

      if (in == null) {
        logger.atWarning()
            .log("Failed to load library %s, resource not found", fileName);
        return null;
      }

      IOUtils.copy(in, out);
    }

    return inputJar;
  }

  @Contract(pure = true)
  private String getFileNameWithoutExtension(@NotNull String fileName) {
    final String[] fileNameParts = fileName.split("\\.");

    if (fileNameParts.length == 1) {
      return fileName;
    } else {
      return fileNameParts[fileNameParts.length - 1];
    }
  }

  @SneakyThrows
  private void addJarToClassLoader(File outputJar) {
    logger.atInfo()
        .log("Adding remapped library to classloader");
    try {
      final SurfLibJoinClassLoader surfLibJoinClassLoader = getOrCreateSurfLibClassLoader();
      final SurfLibClassLoader surfLibClassLoader = new SurfLibClassLoader(
          outputJar.toURI().toURL());
      surfLibJoinClassLoader.addDelegateClassLoader(surfLibClassLoader);
    } catch (IOException e) {
      logger.atWarning()
          .withCause(e)
          .log("Failed to add remapped library to classloader");
    }
  }

  @SneakyThrows
  private SurfLibJoinClassLoader getOrCreateSurfLibClassLoader() {
    final URLClassLoader libraryLoader = LibReflection.PAPER_PLUGIN_CLASS_LOADER_PROXY.getLibraryLoader(
        pluginClassLoader);

    if (libraryLoader instanceof SurfLibJoinClassLoader surfLoader) {
      return surfLoader;
    }

    final SurfLibJoinClassLoader surfLoader = new SurfLibJoinClassLoader(libraryLoader);
    final Field field = PaperPluginClassLoader.class.getDeclaredField("libraryLoader");
    field.setAccessible(true);

    final Unsafe unsafe = SurfUtil.getUnsafe();
    final long offset = unsafe.objectFieldOffset(field);
    unsafe.putObject(pluginClassLoader, offset, surfLoader);

    return surfLoader;
  }

  private static class SurfLibClassLoader extends URLClassLoader {

    public SurfLibClassLoader(URL... urls) {
      super(urls);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private class SurfLibJoinClassLoader extends URLClassLoader implements
      ConfiguredPluginClassLoader {

    private final List<URLClassLoader> delegateClassLoaders;

    /**
     * Instantiates a new Join class loader.
     *
     * @param parent               the parent
     * @param delegateClassLoaders the delegate class loaders
     */
    public SurfLibJoinClassLoader(URLClassLoader parent, URLClassLoader... delegateClassLoaders) {
      super(parent.getURLs(), parent);

      this.delegateClassLoaders = new ArrayList<>(Arrays.stream(delegateClassLoaders).toList());
    }

    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
      final String path = name.replace('.', '/') + ".class";
      final URL url = findResource(path);
      final ByteBuffer byteCode;

      if (url == null) {
        throw new ClassNotFoundException(name);
      }

      try {
        byteCode = loadResource(url);
      } catch (IOException exception) {
        throw new ClassNotFoundException(name, exception);
      }

      return defineClass(name, byteCode, (ProtectionDomain) null);
    }

    @Override
    public URL findResource(String name) {
      for (ClassLoader delegateClassLoader : delegateClassLoaders) {
        final URL resource = delegateClassLoader.getResource(name);

        if (resource != null) {
          return resource;
        }
      }

      return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
      final Vector<URL> vector = new Vector<>();

      for (ClassLoader delegateClassLoader : delegateClassLoaders) {
        final Enumeration<URL> resources = delegateClassLoader.getResources(name);

        while (resources.hasMoreElements()) {
          vector.add(resources.nextElement());
        }
      }

      return vector.elements();
    }

    /**
     * Load resource byte buffer.
     *
     * @param url the url
     * @return the byte buffer
     * @throws IOException the io exception
     */
    private @NotNull ByteBuffer loadResource(URL url) throws IOException {
      try (final InputStream stream = url.openStream()) {
        int initialBufferCapacity = Math.min(0x40000, stream.available() + 1);

        if (initialBufferCapacity <= 2) {
          initialBufferCapacity = 0x10000;
        } else {
          initialBufferCapacity = Math.max(initialBufferCapacity, 0x200);
        }

        ByteBuffer buffer = ByteBuffer.allocate(initialBufferCapacity);

        while (true) {
          if (!buffer.hasRemaining()) {
            final ByteBuffer newBuf = ByteBuffer.allocate(buffer.capacity() * 2);

            buffer.flip();
            newBuf.put(buffer);
            buffer = newBuf;
          }

          int length = stream.read(buffer.array(), buffer.position(), buffer.remaining());

          if (length <= 0) {
            break;
          }

          buffer.position(buffer.position() + length);
        }

        buffer.flip();

        return buffer;
      }
    }

    public void addDelegateClassLoader(URLClassLoader delegateClassLoader) {
      delegateClassLoaders.add(delegateClassLoader);
    }

    @Override
    public PluginMeta getConfiguration() {
      return pluginClassLoader.getConfiguration();
    }

    @Override
    public Class<?> loadClass(@NotNull String name, boolean resolve, boolean checkGlobal,
        boolean checkLibraries) throws ClassNotFoundException {
      return pluginClassLoader.loadClass(name, resolve, checkGlobal, checkLibraries);
    }

    @Override
    public void init(JavaPlugin plugin) {
      pluginClassLoader.init(plugin);
    }

    @Override
    public @Nullable JavaPlugin getPlugin() {
      return pluginClassLoader.getPlugin();
    }

    @Override
    public @Nullable PluginClassLoaderGroup getGroup() {
      return pluginClassLoader.getGroup();
    }
  }
}
