package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.util.FailureCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContext

/**
 * A refreshed context together with the driver of its lifecycle hooks and the durations of its
 * startup phases.
 */
internal class ManagedContext(
    val context: AnnotationConfigApplicationContext,
    val hooks: LifecycleHookRunner,
    val timings: PhaseTimings = PhaseTimings(),
)

/**
 * The bootstrap context of one plugin, built in Paper's bootstrap phase.
 *
 * Created by [SurfContextManager.bootstrap]. It holds only the plugin's components that implement
 * [dev.slne.surf.api.spring.SurfBootstrapLifecycle] or are annotated with
 * [dev.slne.surf.api.spring.BootstrapComponent]; [SurfContextManager.start] later builds the
 * plugin's full context below it.
 */
class PluginBootstrapContext internal constructor(
    val descriptor: PluginDescriptor,
    internal val managed: ManagedContext,
) {

    /**
     * The bootstrap context itself.
     */
    val context: AnnotationConfigApplicationContext get() = managed.context

    /**
     * Runs the bootstrap hooks with the plugin's class loader as thread context class loader, as
     * the [StartupPhase.BOOTSTRAP] step.
     */
    suspend fun bootstrap() {
        withContext(ContextClassLoaderElement(descriptor.classLoader)) {
            managed.measure(StartupPhase.BOOTSTRAP, descriptor.name) { managed.hooks.bootstrap() }
        }
    }
}

/**
 * The Spring contexts of one plugin.
 *
 * Created by [SurfContextManager.start]. From the root downwards, a plugin owns its bootstrap
 * context (on Paper only), its import context with the exports of its direct dependencies, and its
 * own [context]. The phase functions run the hooks of the bootstrap context's beans first, then
 * those of [context], and `disable` in reverse. The hooks run in the caller's coroutine with the
 * elements of [coroutineScope] except its job and dispatcher, so the plugin's class loader is the
 * thread context class loader whichever thread they resume on.
 *
 * [ContextPhaseListener] beans of [context] run after the enable hooks and before the disable
 * hooks.
 *
 * Each phase function runs as the startup step of its [StartupPhase], tagged with the plugin's
 * name, on [context]'s application startup. After the enable phase, one info line reports the
 * durations of the context builds and of the bootstrap, load and enable phases.
 *
 * [coroutineScope] is the plugin's `CoroutineScope` bean. It is cancelled once the disable hooks
 * have run, or when the plugin's contexts close without them, whichever comes first.
 *
 * @property descriptor the plugin this context belongs to
 * @property exports the beans this plugin hands to the plugins depending on it
 */
class PluginContext internal constructor(
    val descriptor: PluginDescriptor,
    private val bootstrap: ManagedContext?,
    private val importContext: GenericApplicationContext,
    private val own: ManagedContext,
    val exports: List<PluginExport>,
    val coroutineScope: CoroutineScope,
) {

    private val log = LogFactory.getLog(PluginContext::class.java)
    private val hookContext = hookElements(coroutineScope)
    private val managed = listOfNotNull(bootstrap, own)

    /**
     * The plugin's own context.
     */
    val context: AnnotationConfigApplicationContext get() = own.context

    /**
     * Every context that holds the plugin's own beans: its bootstrap context, if any, and
     * [context].
     */
    val beanContexts: List<AnnotationConfigApplicationContext>
        get() = managed.map(
            ManagedContext::context,
        )

    /**
     * The phase durations of the bootstrap context, if any, and of [context].
     */
    internal val startupTimings: List<PhaseTimings> get() = managed.map(ManagedContext::timings)

    /**
     * Runs the load hooks.
     *
     * A plugin without a bootstrap context runs the bootstrap hooks of [context] first, so
     * [dev.slne.surf.api.spring.SurfBootstrapLifecycle.onBootstrap] runs before `onLoad` on every
     * platform.
     */
    suspend fun load() {
        withContext(hookContext) {
            if (bootstrap == null) {
                own.measure(StartupPhase.BOOTSTRAP, descriptor.name) { own.hooks.bootstrap() }
            }

            own.measure(StartupPhase.LOAD, descriptor.name) {
                for (context in managed) {
                    context.hooks.load()
                }
            }
        }
    }

    /**
     * Runs the enable hooks, then logs the startup durations at info level.
     */
    suspend fun enable() {
        withContext(hookContext) {
            own.measure(StartupPhase.ENABLE, descriptor.name) {
                for (context in managed) {
                    context.hooks.enable()
                }

                own.context.runAfterEnable()
            }
        }

        log.info(describeStartup("Plugin '${descriptor.name}'", *startupTimings.toTypedArray()))
    }

    /**
     * Runs the disable hooks in reverse order, then cancels [coroutineScope]; failures of the hooks
     * are logged, not thrown.
     */
    suspend fun disable() {
        try {
            withContext(hookContext) {
                own.measure(StartupPhase.DISABLE, descriptor.name) {
                    own.context.runBeforeDisable("plugin '${descriptor.name}'")

                    for (context in managed.asReversed()) {
                        context.hooks.disable()
                    }
                }
            }
        } finally {
            coroutineScope.cancel("Plugin '${descriptor.name}' is disabled")
        }
    }

    /**
     * Closes the plugin's context, its import context and its bootstrap context, in that order.
     *
     * The plugin's contexts destroy the plugin's own beans, exports included. The import context
     * only drops its references, so the exports of this plugin's dependencies stay intact.
     */
    internal fun close() {
        coroutineScope.cancel("The contexts of plugin '${descriptor.name}' are closed")
        withContextClassLoader(descriptor.classLoader) {
            closeContexts(own.context, importContext, bootstrap?.context)
        }
    }
}

/**
 * Closes every non-null context in order, even if one of them fails, and rethrows the first
 * failure.
 */
internal fun closeContexts(vararg contexts: GenericApplicationContext?) {
    val failures = FailureCollector()

    for (context in contexts) {
        try {
            context?.close()
        } catch (exception: Throwable) {
            failures.add(exception)
        }
    }

    failures.throwIfAny()
}
