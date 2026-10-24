package dev.slne.surf.api.minestom.server.bootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.gremlin.runtime.DependencyCache;
import xyz.jpenilla.gremlin.runtime.DependencyResolver;
import xyz.jpenilla.gremlin.runtime.DependencySet;

/**
 * The bootstrap agent of a surf Minestom server jar, registered as its
 * {@code Launcher-Agent-Class}.
 *
 * <p>The server jar contains only the server's own code. The remaining libraries (Minestom,
 * Adventure, fastutil, ...) are listed in the {@value #DEPENDENCIES_FILE} gremlin writes at build
 * time, downloaded into {@code libraries/} on the first start and appended to the system class
 * path before {@code main} runs.</p>
 *
 * <p>Plugins listed in the {@value ServerPlugins#ATTRIBUTE} manifest attribute are appended to the
 * system class path as well, see {@link ServerPlugins}.</p>
 *
 * <p>A jar has a single {@code Launcher-Agent-Class}, so the agents of installed features (e.g.
 * the one LuckPerms loads its libraries with) are listed as delegate agents in
 * {@value #DELEGATE_AGENTS_ATTRIBUTE}. The bootstrap agent calls them in listed order after the
 * libraries and plugins are on the system class path. A delegate agent that installs class
 * transformers, such as {@code SurfMixinAgent}, has to be listed first so the transformer exists
 * before any other agent loads a Minestom class.</p>
 */
@NullMarked
public final class SurfMinestomBootstrap {

    /**
     * The gremlin dependency set listing the libraries to download, at the root of the server jar.
     */
    public static final String DEPENDENCIES_FILE = "dependencies.txt";

    /**
     * The manifest attribute naming the agents to call after the libraries are installed.
     */
    public static final String DELEGATE_AGENTS_ATTRIBUTE = "Surf-Delegate-Agent-Classes";

    /**
     * The system property overriding the directory the libraries are downloaded to.
     */
    public static final String LIBRARIES_PROPERTY = "surf.minestom.libraries";

    /**
     * The system property the bootstrap agent sets to the path of the server jar before it calls
     * the delegate agents, so that an agent loaded from a downloaded library still finds the
     * manifest of the server jar.
     */
    public static final String SERVER_JAR_PROPERTY = "surf.minestom.server-jar";

    /**
     * The Minestom server properties that may change at runtime, see
     * {@code SurfMinestomServerBuilder.recommendedServerProperties}. Minestom freezes a property's
     * value when it is first read unless {@code <name>.mutable} is set before that.
     */
    private static final List<String> MUTABLE_SERVER_PROPERTIES = List.of("minestom.keep-alive-delay");

    /**
     * Minestom's own JVM shutdown hook. The surf api stops the server from a hook of its own, which
     * also runs the server's shutdown actions, so Minestom's is turned off unless set with
     * {@code -D}.
     */
    private static final String SHUTDOWN_ON_SIGNAL_PROPERTY = "minestom.shutdown-on-signal";

    private static final BootstrapLogger LOGGER = new BootstrapLogger();

    private SurfMinestomBootstrap() {
    }

    /**
     * Called by the JVM for the {@code Launcher-Agent-Class} of a jar started with {@code java -jar}.
     */
    public static void agentmain(final @Nullable String args, final Instrumentation instrumentation) {
        try {
            for (final String property : MUTABLE_SERVER_PROPERTIES) {
                if (System.getProperty(property + ".mutable") == null) {
                    System.setProperty(property + ".mutable", "true");
                }
            }
            if (System.getProperty(SHUTDOWN_ON_SIGNAL_PROPERTY) == null) {
                System.setProperty(SHUTDOWN_ON_SIGNAL_PROPERTY, "false");
            }

            final Path serverJar = ownJar();
            System.setProperty(SERVER_JAR_PROPERTY, serverJar.toString());

            final Attributes manifest = manifest(serverJar);
            installLibraries(instrumentation);

            final @Nullable String serverPlugins = manifest.getValue(ServerPlugins.ATTRIBUTE);
            if (serverPlugins != null) {
                ServerPlugins.install(serverPlugins, instrumentation, LOGGER);
            }

            callDelegateAgents(manifest.getValue(DELEGATE_AGENTS_ATTRIBUTE), args, instrumentation);
        } catch (final Throwable e) {
            // An exception thrown from the bootstrap agent is buried under the JVM's own messages
            LOGGER.error("Failed to start the server", e);
            System.exit(1);
        }
    }

    /**
     * Called by the JVM when the server jar is passed as {@code -javaagent}.
     */
    public static void premain(final @Nullable String args, final Instrumentation instrumentation) {
        agentmain(args, instrumentation);
    }

    private static void installLibraries(final Instrumentation instrumentation) {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();

        // Jars with every library shaded in have no dependency set
        if (loader.getResource(DEPENDENCIES_FILE) == null) {
            return;
        }

        final DependencySet dependencies = DependencySet.readDefault(loader);
        final DependencyCache cache = new DependencyCache(
            Path.of(System.getProperty(LIBRARIES_PROPERTY, "libraries"))
        );

        try (DependencyResolver resolver = new DependencyResolver(LOGGER)) {
            // The resolved set is unordered; a fixed order keeps the class path identical between starts
            for (final Path jar : resolver.resolve(dependencies, cache).jarFiles().stream().sorted().toList()) {
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

    private static Path ownJar() {
        try {
            return Path.of(SurfMinestomBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Failed to locate the server jar", e);
        }
    }

    private static Attributes manifest(final Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            final @Nullable Manifest manifest = jarFile.getManifest();
            return manifest == null ? new Attributes() : manifest.getMainAttributes();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read the manifest of the server jar", e);
        }
    }
}
