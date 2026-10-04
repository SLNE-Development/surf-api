package dev.slne.surf.api.minestom.server.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Puts the plugins the server's own code uses on the system class path.
 *
 * <p>A plugin in {@code plugins/} normally gets a class loader of its own, which the server cannot
 * see into. The plugins the server declares in {@value #ATTRIBUTE} (and the plugins they depend
 * on) are appended to the system class path instead, and the plugins feature loads them from
 * there. Their jar paths are handed to it through the {@value #LOADED_PROPERTY} system
 * property.</p>
 */
@NullMarked
final class ServerPlugins {

    /** The manifest attribute listing the plugin ids, comma separated, optional ones ending in {@code ?}. */
    static final String ATTRIBUTE = "Surf-Plugin-Dependencies";

    /** The system property overriding the plugin directory, which has to match {@code withPlugins}. */
    static final String DIRECTORY_PROPERTY = "surf.minestom.plugins";

    /** The system property the appended jars are listed in, separated by the path separator. */
    static final String LOADED_PROPERTY = "surf.minestom.server-plugins";

    private static final String PLUGIN_FILE = "minestom-plugin.json";
    private static final String CLASSPATH_INDEX = "META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin";
    private static final String CLASSPATH_DIRECTORY = "META-INF/surf-minestom-plugins/";

    private ServerPlugins() {
    }

    static void install(final String declared, final Instrumentation instrumentation, final BootstrapLogger logger) {
        final Path directory = Path.of(System.getProperty(DIRECTORY_PROPERTY, "plugins")).toAbsolutePath().normalize();
        final Map<String, PluginJar> available = scan(directory, logger);
        final Set<String> onClassPath = classPathPluginIds();

        final Map<String, PluginJar> selected = new LinkedHashMap<>();
        final Deque<Requirement> pending = new ArrayDeque<>(parse(declared, "the server"));
        while (!pending.isEmpty()) {
            final Requirement requirement = pending.removeFirst();
            if (selected.containsKey(requirement.id()) || onClassPath.contains(requirement.id())) {
                continue;
            }

            final @Nullable PluginJar jar = available.get(requirement.id());
            if (jar == null) {
                if (!requirement.optional()) {
                    throw new IllegalStateException(
                        "The plugin " + requirement.id() + ", which " + requirement.requiredBy()
                            + " depends on, is missing in " + directory
                    );
                }
                continue;
            }

            selected.put(requirement.id(), jar);
            pending.addAll(jar.dependencies());
        }

        final List<String> paths = new ArrayList<>();
        for (final PluginJar jar : selected.values()) {
            try {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.path().toFile()));
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to add " + jar.path() + " to the class path", e);
            }
            paths.add(jar.path().toString());
        }

        if (!paths.isEmpty()) {
            logger.info("Added the plugins " + String.join(", ", selected.keySet()) + " to the server's class path");
        }
        System.setProperty(LOADED_PROPERTY, String.join(File.pathSeparator, paths));
    }

    private static Map<String, PluginJar> scan(final Path directory, final BootstrapLogger logger) {
        if (!Files.isDirectory(directory)) {
            return Map.of();
        }

        final Map<String, PluginJar> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (final Path path : files.sorted().toList()) {
                if (!Files.isRegularFile(path) || !path.getFileName().toString().toLowerCase().endsWith(".jar")) {
                    continue;
                }

                try (JarFile jar = new JarFile(path.toFile())) {
                    final @Nullable JarEntry entry = jar.getJarEntry(PLUGIN_FILE);
                    if (entry == null) {
                        continue;
                    }

                    final Map<?, ?> meta = readObject(jar.getInputStream(entry));
                    final String id = String.valueOf(meta.get("id"));
                    result.putIfAbsent(id, new PluginJar(path, dependencies(meta, id)));
                } catch (final IOException | RuntimeException e) {
                    // Reported again by the plugins feature, which skips the jar as well
                    logger.warn("Could not read the plugin " + path.getFileName(), e);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to list " + directory, e);
        }
        return result;
    }

    /** The ids of the plugins shaded into the server, which need no jar from the directory. */
    private static Set<String> classPathPluginIds() {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        final Set<String> ids = new HashSet<>();
        try {
            for (final URL index : Collections.list(loader.getResources(CLASSPATH_INDEX))) {
                try (InputStream stream = index.openStream()) {
                    for (final String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                        final String mainClass = line.split("#", 2)[0].trim();
                        if (mainClass.isEmpty()) {
                            continue;
                        }

                        final @Nullable URL meta = loader.getResource(CLASSPATH_DIRECTORY + mainClass + ".json");
                        if (meta != null) {
                            ids.add(String.valueOf(readObject(meta.openStream()).get("id")));
                        }
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read the plugins on the class path", e);
        }
        return ids;
    }

    private static List<Requirement> dependencies(final Map<?, ?> meta, final String id) {
        final List<Requirement> result = new ArrayList<>();
        if (meta.get("dependencies") instanceof final List<?> dependencies) {
            for (final Object dependency : dependencies) {
                if (dependency instanceof final Map<?, ?> map) {
                    result.add(new Requirement(
                        String.valueOf(map.get("id")),
                        Boolean.TRUE.equals(map.get("optional")),
                        "the plugin " + id
                    ));
                }
            }
        }
        return result;
    }

    private static List<Requirement> parse(final String declared, final String requiredBy) {
        final Set<Requirement> result = new LinkedHashSet<>();
        for (final String entry : declared.split(",")) {
            final String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final boolean optional = trimmed.endsWith("?");
            result.add(new Requirement(optional ? trimmed.substring(0, trimmed.length() - 1) : trimmed, optional, requiredBy));
        }
        return List.copyOf(result);
    }

    private static Map<?, ?> readObject(final InputStream input) throws IOException {
        try (input) {
            if (MiniJson.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8)) instanceof final Map<?, ?> map) {
                return map;
            }
            throw new IllegalArgumentException(PLUGIN_FILE + " is no JSON object");
        }
    }

    private record PluginJar(Path path, List<Requirement> dependencies) {
    }

    private record Requirement(String id, boolean optional, String requiredBy) {
    }
}
