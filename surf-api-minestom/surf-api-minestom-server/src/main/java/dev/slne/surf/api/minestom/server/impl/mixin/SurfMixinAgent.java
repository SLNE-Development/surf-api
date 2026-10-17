package dev.slne.surf.api.minestom.server.impl.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Installs Mixin into a surf Minestom server.
 *
 * <p>Called by the bootstrap agent as its first delegate agent: after the libraries are on the
 * system class path, before any other agent or {@code main} runs. The transformer must exist
 * before the first Minestom class is loaded, because classes loaded earlier are never transformed.</p>
 *
 * <p>The mixin configurations come from the {@value #CONFIGS_ATTRIBUTE} manifest attribute of the
 * server jar, which the surf Gradle plugin writes from the installed features and the mixin
 * configurations the server registers. The server jar is the one the bootstrap agent reports in
 * {@code surf.minestom.server-jar}, since this class may come from a downloaded library, or else
 * the jar this class was loaded from. Mixin can be installed once per JVM.</p>
 *
 * <p>In addition, the optional compatibility configurations of surf are applied for each library
 * whose marker class is on the system class path, see {@link #compatibilityConfigs()}. Servers
 * need not list them in the manifest, and a server without such a library gets no warning about a
 * missing target. Their injections are not required, so a library version that no longer needs a
 * fix is left untransformed without an error. A manifest that lists no configuration at all still
 * fails the installation.</p>
 *
 * <p>A required mixin whose target was loaded before the installation fails the installation. A
 * transformation that fails later makes the JVM load the class untransformed; such classes are
 * reported by {@link #failedTransformations()}.</p>
 */
@NullMarked
public final class SurfMixinAgent {

    /**
     * The manifest attribute listing the mixin configurations to apply, comma separated.
     */
    public static final String CONFIGS_ATTRIBUTE = "Surf-Mixin-Configs";

    /**
     * The system property {@code SurfMinestomBootstrap} sets to the path of the server jar.
     */
    private static final String SERVER_JAR_PROPERTY = "surf.minestom.server-jar";

    /**
     * Optional configurations that fix the compatibility of a third-party library with Minestom.
     */
    private static final List<CompatibilityConfig> COMPATIBILITY_CONFIGS = List.of(
        new CompatibilityConfig(
            "mixins.surf-api-minestom-server-miniplaceholders.json",
            "io/github/miniplaceholders/minestom/command/MinestomCommand.class"
        )
    );

    private static volatile boolean installed;

    private static volatile @Nullable Throwable installationFailure;

    private SurfMixinAgent() {
    }

    /**
     * The surf compatibility mixin configurations whose marker class is on the system class path.
     *
     * <p>Each configuration targets a third-party class, so it is applied only when that library is
     * present; the Mixin target of a missing library would be reported as not found.</p>
     */
    public static List<String> compatibilityConfigs() {
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return COMPATIBILITY_CONFIGS.stream()
            .filter(config -> classLoader.getResource(config.markerResource()) != null)
            .map(CompatibilityConfig::config)
            .toList();
    }

    /**
     * Installs Mixin with the mixin configurations listed in the manifest, plus the
     * {@linkplain #compatibilityConfigs() compatibility configurations} of the libraries on the
     * class path.
     *
     * @throws IllegalStateException when the manifest lists no mixin configuration or Mixin is
     * already installed
     */
    public static void agentmain(final @Nullable String args, final Instrumentation instrumentation) {
        final List<String> configs = configsFromManifest();
        if (configs.isEmpty()) {
            throw new IllegalStateException("The server jar lists no mixin configurations in " + CONFIGS_ATTRIBUTE);
        }

        final List<String> all = new ArrayList<>(configs);
        for (final String compatibilityConfig : compatibilityConfigs()) {
            if (!all.contains(compatibilityConfig)) {
                all.add(compatibilityConfig);
            }
        }
        install(instrumentation, all);
    }

    /**
     * Delegates to {@link #agentmain}.
     */
    public static void premain(final @Nullable String args, final Instrumentation instrumentation) {
        agentmain(args, instrumentation);
    }

    /**
     * Installs the Mixin transformer through {@code instrumentation} and applies {@code configs}.
     *
     * <p>Mixin cannot be initialised again after a partial installation, so a failed installation
     * is final: {@link #isInstalled()} stays {@code false} and every later call fails with the
     * original failure as cause.</p>
     *
     * @throws IllegalStateException when Mixin is already installed or an earlier installation
     * failed
     */
    public static synchronized void install(final Instrumentation instrumentation, final Collection<String> configs) {
        if (installed) {
            throw new IllegalStateException("Mixin is already installed");
        }
        final @Nullable Throwable earlierFailure = installationFailure;
        if (earlierFailure != null) {
            throw new IllegalStateException("Mixin installation failed earlier and cannot be retried", earlierFailure);
        }

        try {
            final Set<String> loaded = Arrays.stream(instrumentation.getAllLoadedClasses())
                .map(Class::getName)
                .collect(Collectors.toUnmodifiableSet());

            InstrumentationMixinService.setInstrumentation(instrumentation, loaded);
            MixinBootstrap.init();
            MixinExtrasBootstrap.init();
            configs.forEach(Mixins::addConfiguration);
            advancePhases();
        } catch (final Throwable failure) {
            installationFailure = failure;
            throw failure;
        }
        installed = true;
    }

    /**
     * Whether Mixin was installed completely into this JVM.
     */
    public static boolean isInstalled() {
        return installed;
    }

    /**
     * Binary names of the classes that were loaded untransformed because a mixin failed to apply.
     */
    public static List<String> failedTransformations() {
        return InstrumentationMixinService.failures();
    }

    /**
     * Moves Mixin through {@code INIT} to {@code DEFAULT}. Mixin selects its configurations on a
     * phase change, which no launcher triggers here.
     */
    private static void advancePhases() {
        try {
            final var gotoPhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            gotoPhase.setAccessible(true);
            gotoPhase.invoke(null, MixinEnvironment.Phase.INIT);
            gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to advance the Mixin phases", e);
        }
    }

    private static List<String> configsFromManifest() {
        final @Nullable Path jar = serverJar();
        if (jar == null || !Files.isRegularFile(jar)) {
            return List.of();
        }

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            final @Nullable Manifest manifest = jarFile.getManifest();
            final @Nullable String configs = manifest == null ? null : manifest.getMainAttributes().getValue(CONFIGS_ATTRIBUTE);
            if (configs == null) {
                return List.of();
            }

            return Arrays.stream(configs.split(","))
                .map(String::trim)
                .filter(config -> !config.isEmpty())
                .toList();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read the mixin configurations from the server jar", e);
        }
    }

    /**
     * A mixin configuration that applies only when {@code markerResource}, the class file of the
     * library it targets, is on the class path.
     */
    private record CompatibilityConfig(String config, String markerResource) {
    }

    private static @Nullable Path serverJar() {
        final @Nullable String reported = System.getProperty(SERVER_JAR_PROPERTY);
        if (reported != null) {
            return Path.of(reported);
        }

        final @Nullable CodeSource codeSource = SurfMixinAgent.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }

        try {
            return Path.of(codeSource.getLocation().toURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Failed to locate the server jar", e);
        }
    }
}
