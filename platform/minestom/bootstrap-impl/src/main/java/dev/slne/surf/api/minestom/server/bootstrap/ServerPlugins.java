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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Puts the plugins the server's own code uses on the system class path.
 *
 * <p>A plugin in {@code plugins/} normally gets a class loader of its own, which the server cannot
 * see into. The plugins the server declares in {@value #ATTRIBUTE} (and the plugins they depend
 * on) are appended to the system class path instead, and the plugins feature loads them from
 * there. The bootstrap passes their jar paths to the plugins feature in the
 * {@value #LOADED_PROPERTY} system property.</p>
 */
@NullMarked
final class ServerPlugins {

    /**
     * The manifest attribute listing the plugin IDs, comma separated. The ID of an optional plugin
     * ends in {@code ?}.
     */
    static final String ATTRIBUTE = "Surf-Plugin-Dependencies";

    /**
     * The system property overriding the {@code plugins} directory. It has to match the directory
     * configured in {@code withPlugins}.
     */
    static final String DIRECTORY_PROPERTY = "surf.minestom.plugins";

    /**
     * The system property the appended jars are listed in, separated by the path separator.
     */
    static final String LOADED_PROPERTY = "surf.minestom.server-plugins";

    private static final String PLUGIN_FILE = "minestom-plugin.json";
    private static final String CLASSPATH_INDEX = "META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin";
    private static final String CLASSPATH_DIRECTORY = "META-INF/surf-minestom-plugins/";

    /**
     * The format of a plugin ID, kept in sync with {@code PluginMeta.ID_REGEX}.
     */
    private static final Pattern PLUGIN_ID = Pattern.compile("[a-z][a-z0-9-_]{0,63}");

    private ServerPlugins() {
    }

    static void install(final String declared, final Instrumentation instrumentation, final BootstrapLogger logger) {
        final Path directory = Path.of(System.getProperty(DIRECTORY_PROPERTY, "plugins")).toAbsolutePath().normalize();
        final Map<String, PluginJar> available = scan(directory, logger);
        final Set<String> onClassPath = classPathPluginIds();

        final Map<String, PluginJar> selected = new LinkedHashMap<>();
        for (final Requirement root : parse(declared, "the server")) {
            final Map<String, PluginJar> added = new LinkedHashMap<>();
            final @Nullable Requirement missing = resolve(root, available, onClassPath, selected, added);
            if (missing == null) {
                selected.putAll(added);
                continue;
            }

            final String message = "The plugin " + missing.id() + ", which " + missing.requiredBy()
                + " depends on, is missing in " + directory;
            if (!root.optional()) {
                throw new IllegalStateException(message);
            }
            logger.warn("Not adding the optional plugin " + root.id() + " to the server's class path. " + message);
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

    /**
     * Collects {@code root} and the plugins it requires into {@code added}, skipping the ones
     * already in {@code selected} or on the class path.
     *
     * @return the first required plugin that is missing, or {@code null} when everything
     * {@code root} needs is available; a missing {@code root} that is optional is not reported
     */
    private static @Nullable Requirement resolve(
        final Requirement root,
        final Map<String, PluginJar> available,
        final Set<String> onClassPath,
        final Map<String, PluginJar> selected,
        final Map<String, PluginJar> added
    ) {
        final Deque<Requirement> pending = new ArrayDeque<>(List.of(root));
        while (!pending.isEmpty()) {
            final Requirement requirement = pending.removeFirst();
            final String id = requirement.id();
            if (selected.containsKey(id) || added.containsKey(id) || onClassPath.contains(id)) {
                continue;
            }

            final @Nullable PluginJar jar = available.get(id);
            if (jar == null) {
                if (!requirement.optional()) {
                    return requirement;
                }
                continue;
            }

            added.put(id, jar);
            pending.addAll(jar.dependencies());
        }
        return null;
    }

    private static Map<String, PluginJar> scan(final Path directory, final BootstrapLogger logger) {
        if (!Files.isDirectory(directory)) {
            return Map.of();
        }

        final Map<String, PluginJar> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (final Path path : files.sorted().toList()) {
                if (!Files.isRegularFile(path) || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    continue;
                }

                try (JarFile jar = new JarFile(path.toFile())) {
                    final @Nullable JarEntry entry = jar.getJarEntry(PLUGIN_FILE);
                    if (entry == null) {
                        continue;
                    }

                    final Map<?, ?> meta = readObject(jar.getInputStream(entry));
                    if (!(meta.get("id") instanceof final String id) || !PLUGIN_ID.matcher(id).matches()) {
                        logger.warn("Skipping " + path.getFileName() + ", its " + PLUGIN_FILE + " has no valid plugin id");
                        continue;
                    }

                    result.putIfAbsent(id, new PluginJar(path, dependencies(meta, id)));
                } catch (final IOException | RuntimeException e) {
                    // The plugins feature reports the same failure when it skips the jar
                    logger.warn("Could not read the plugin " + path.getFileName(), e);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to list " + directory, e);
        }
        return result;
    }

    /**
     * The IDs of the plugins shaded into the server, which need no jar from the {@code plugins}
     * directory.
     */
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
