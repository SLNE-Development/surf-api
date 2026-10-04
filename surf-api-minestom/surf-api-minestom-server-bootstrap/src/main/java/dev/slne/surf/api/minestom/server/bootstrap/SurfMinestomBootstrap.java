package dev.slne.surf.api.minestom.server.bootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.gremlin.runtime.DependencyCache;
import xyz.jpenilla.gremlin.runtime.DependencyResolver;
import xyz.jpenilla.gremlin.runtime.DependencySet;

/**
 * The {@code Launcher-Agent-Class} of a surf Minestom server jar.
 *
 * <p>The server jar only contains the server's own code. Minestom, Adventure, fastutil and every
 * other library are listed in the {@value #DEPENDENCIES_FILE} written by gremlin at build time,
 * downloaded into {@code libraries/} on the first start and appended to the system class path
 * before {@code main} runs, so they are loaded exactly as if they had been shaded.</p>
 *
 * <p>The plugins the server's own code uses, listed in the {@value ServerPlugins#ATTRIBUTE}
 * manifest attribute, are put on the system class path too, see {@link ServerPlugins}.</p>
 *
 * <p>Since a jar has only one launcher agent, the agents of the installed features (e.g. the one
 * LuckPerms loads its own libraries with) are listed in the {@value #DELEGATE_AGENTS_ATTRIBUTE}
 * manifest attribute and called afterwards, once their classes can be found.</p>
 */
@NullMarked
public final class SurfMinestomBootstrap {

    /** The gremlin dependency set, at the root of the server jar. */
    public static final String DEPENDENCIES_FILE = "dependencies.txt";

    /** The manifest attribute naming the agents to call after the libraries are installed. */
    public static final String DELEGATE_AGENTS_ATTRIBUTE = "Surf-Delegate-Agent-Classes";

    /** The system property overriding the directory the libraries are downloaded to. */
    public static final String LIBRARIES_PROPERTY = "surf.minestom.libraries";

    private static final BootstrapLogger LOGGER = new BootstrapLogger();

    private SurfMinestomBootstrap() {
    }

    /** Called by the JVM for the {@code Launcher-Agent-Class} of a jar started with {@code java -jar}. */
    public static void agentmain(final @Nullable String args, final Instrumentation instrumentation) {
        try {
            final Attributes manifest = ownManifest();
            installLibraries(instrumentation);

            final @Nullable String serverPlugins = manifest.getValue(ServerPlugins.ATTRIBUTE);
            if (serverPlugins != null) {
                ServerPlugins.install(serverPlugins, instrumentation, LOGGER);
            }

            callDelegateAgents(manifest.getValue(DELEGATE_AGENTS_ATTRIBUTE), args, instrumentation);
        } catch (final Throwable e) {
            // Thrown out of a launcher agent, the error is buried under the JVM's own messages
            LOGGER.error("Failed to start the server", e);
            System.exit(1);
        }
    }

    /** Called by the JVM when the server jar is passed as {@code -javaagent}. */
    public static void premain(final @Nullable String args, final Instrumentation instrumentation) {
        agentmain(args, instrumentation);
    }

    private static void installLibraries(final Instrumentation instrumentation) {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();

        // A server jar with every library shaded in has nothing to download
        if (loader.getResource(DEPENDENCIES_FILE) == null) {
            return;
        }

        final DependencySet dependencies = DependencySet.readDefault(loader);
        final DependencyCache cache = new DependencyCache(
            Path.of(System.getProperty(LIBRARIES_PROPERTY, "libraries"))
        );

        try (DependencyResolver resolver = new DependencyResolver(LOGGER)) {
            for (final Path jar : resolver.resolve(dependencies, cache).jarFiles()) {
                try {
                    instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
                } catch (final IOException e) {
                    throw new UncheckedIOException("Failed to add " + jar + " to the class path", e);
                }
            }
        }

        cache.cleanup();
    }

    private static void callDelegateAgents(
        final @Nullable String agents,
        final @Nullable String args,
        final Instrumentation instrumentation
    ) {
        if (agents == null) {
            return;
        }

        Arrays.stream(agents.split(","))
            .map(String::trim)
            .filter(agent -> !agent.isEmpty())
            .forEach(agent -> callAgent(agent, args, instrumentation));
    }

    private static void callAgent(final String className, final @Nullable String args, final Instrumentation instrumentation) {
        try {
            final Class<?> agent = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            Method entryPoint;
            try {
                entryPoint = agent.getMethod("agentmain", String.class, Instrumentation.class);
            } catch (final NoSuchMethodException e) {
                entryPoint = agent.getMethod("premain", String.class, Instrumentation.class);
            }
            entryPoint.invoke(null, args, instrumentation);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException("Agent " + className + " failed", e.getCause());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to call agent " + className, e);
        }
    }

    private static Attributes ownManifest() {
        try {
            final Path jar = Path.of(SurfMinestomBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                final @Nullable Manifest manifest = jarFile.getManifest();
                return manifest == null ? new Attributes() : manifest.getMainAttributes();
            }
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to read the manifest of the server jar", e);
        }
    }
}
