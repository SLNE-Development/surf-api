package dev.slne.surf.api.minestom.server.impl.mixin;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

/**
 * Runs Mixin on top of {@link Instrumentation} instead of a launcher.
 *
 * <p>Found by Mixin through {@code META-INF/services}; valid only once
 * {@link #setInstrumentation} was called. The transformer Mixin offers is registered as a
 * non-retransforming {@link ClassFileTransformer}, so it only affects classes loaded afterwards;
 * a required mixin whose target was loaded before fails Mixin's own early-load check.</p>
 *
 * <p>Classes and resources are read from the system class loader, which holds the server and its
 * libraries. A failed transformation is logged and recorded in {@link #failures()} before it is
 * rethrown, since the JVM swallows the exception and loads the untransformed class.</p>
 */
@NullMarked
public final class InstrumentationMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
        "org/spongepowered/asm/",
        "com/llamalad7/mixinextras/"
    );

    private static final List<String> FAILURES = new CopyOnWriteArrayList<>();

    private static volatile @Nullable Instrumentation instrumentation;

    /** Binary names of the classes loaded before Mixin was installed. */
    private static volatile Set<String> loadedBeforeInstall = Set.of();

    private static volatile @Nullable IMixinTransformer transformer;

    static void setInstrumentation(final Instrumentation instrumentation) {
        if (InstrumentationMixinService.instrumentation != null) {
            throw new IllegalStateException("Instrumentation has already been initialized");
        }

        InstrumentationMixinService.instrumentation = instrumentation;
    }

    /** The installed transformer, or {@code null} before Mixin offered it. */
    static @Nullable IMixinTransformer transformer() {
        return transformer;
    }

    static void setInstrumentation(final Instrumentation instrumentation, final Set<String> loadedClasses) {
        loadedBeforeInstall = loadedClasses;
        setInstrumentation(instrumentation);
    }

    /** Binary names of the classes whose transformation failed, in the order they failed. */
    static List<String> failures() {
        return List.copyOf(FAILURES);
    }

    @Override
    public String getName() {
        return "SurfMinestomInstrumentation";
    }

    @Override
    public boolean isValid() {
        return instrumentation != null;
    }

    @Override
    public void offer(final IMixinInternal internal) {
        super.offer(internal);

        if (internal instanceof IMixinTransformerFactory factory) {
            if (transformer != null) {
                throw new IllegalStateException("Mixin transformer has already been installed");
            }

            final IMixinTransformer created = factory.createTransformer();
            transformer = created;
            requireNonNull(instrumentation, "instrumentation").addTransformer(new TransformerAdapter(created), false);
        }
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public @Nullable ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return ClassTracker.INSTANCE;
    }

    @Override
    protected ILogger createLogger(final String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public @Nullable IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return IFeatureValidator.ALLOW_ALL;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return IAdviceProvider.GENERIC;
    }

    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return CompatibilityLevel.JAVA_8;
    }

    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return CompatibilityLevel.JAVA_25;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        // MixinContainer always adds MixinPlatformAgentDefault itself
        return List.of();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        final @Nullable CodeSource codeSource = InstrumentationMixinService.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return new ContainerHandleURI(URI.create("file:/"));
        }

        try {
            return new ContainerHandleURI(codeSource.getLocation().toURI());
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to determine the server container", e);
        }
    }

    @Override
    public @Nullable InputStream getResourceAsStream(final String name) {
        return loader().getResourceAsStream(name);
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader());
    }

    @Override
    public Class<?> findClass(final String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, loader());
    }

    @Override
    public Class<?> findAgentClass(final String name, final boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }

    @Override
    public ClassNode getClassNode(final String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(final String name, final boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(final String name, final boolean runTransformers, final int readerFlags)
        throws ClassNotFoundException, IOException {
        final byte[] bytes;
        try (InputStream stream = loader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) {
                throw new ClassNotFoundException(name);
            }
            bytes = stream.readAllBytes();
        }

        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, readerFlags);
        return node;
    }

    private static ClassLoader loader() {
        return ClassLoader.getSystemClassLoader();
    }

    private enum ClassTracker implements IClassTracker {
        INSTANCE;

        @Override
        public void registerInvalidClass(final String className) {
        }

        @Override
        public boolean isClassLoaded(final String className) {
            return loadedBeforeInstall.contains(className);
        }

        @Override
        public String getClassRestrictions(final String className) {
            return "";
        }
    }

    private record TransformerAdapter(IMixinTransformer transformer) implements ClassFileTransformer {

        @Override
        public byte @Nullable [] transform(
            final @Nullable Module module,
            final @Nullable ClassLoader loader,
            final @Nullable String className,
            final @Nullable Class<?> classBeingRedefined,
            final @Nullable ProtectionDomain protectionDomain,
            final byte[] classfileBuffer
        ) throws IllegalClassFormatException {
            if (className == null) {
                return null;
            }

            for (final String prefix : EXCLUDED_PREFIXES) {
                if (className.startsWith(prefix)) {
                    return null;
                }
            }

            final String binaryName = className.replace('/', '.');
            try {
                final byte[] result = transformer.transformClassBytes(binaryName, binaryName, classfileBuffer);
                return result == classfileBuffer ? null : result;
            } catch (final Throwable e) {
                FAILURES.add(binaryName);
                System.err.println("[surf-mixin] Transformation of " + binaryName + " failed, it is loaded untransformed");
                e.printStackTrace();
                final var failure = new IllegalClassFormatException("Mixin transformation failed for " + binaryName + ": " + e);
                failure.initCause(e);
                throw failure;
            }
        }
    }
}
