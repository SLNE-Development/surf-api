package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.util.FailureCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContext

/**
 * A refreshed context together with the driver of its lifecycle hooks.
 */
internal class ManagedContext(
    val context: AnnotationConfigApplicationContext,
    val hooks: LifecycleHookRunner,
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
     * Runs the bootstrap hooks with the plugin's class loader as thread context class loader.
     */
    suspend fun bootstrap(): Unit = withContext(ContextClassLoaderElement(descriptor.classLoader)) {
        managed.hooks.bootstrap()
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
 * [ContextPhaseListener] beans of [context] run after the enable hooks and before the disable hooks.
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
    val beanContexts: List<AnnotationConfigApplicationContext> get() = managed.map(ManagedContext::context)

    /**
     * Runs the load hooks.
     *
     * A plugin without a bootstrap context runs the bootstrap hooks of [context] first, so
     * [dev.slne.surf.api.spring.SurfBootstrapLifecycle.onBootstrap] runs before `onLoad` on every
     * platform.
     */
    suspend fun load(): Unit = withContext(hookContext) {
        if (bootstrap == null) own.hooks.bootstrap()
        managed.forEach { it.hooks.load() }
    }

    /**
     * Runs the enable hooks.
     */
    suspend fun enable(): Unit = withContext(hookContext) {
        managed.forEach { it.hooks.enable() }
        own.context.runAfterEnable()
    }

    /**
     * Runs the disable hooks in reverse order, then cancels [coroutineScope]; failures of the hooks
     * are logged, not thrown.
     */
    suspend fun disable() {
        try {
            withContext(hookContext) {
                own.context.runBeforeDisable("plugin '${descriptor.name}'")
                managed.asReversed().forEach { it.hooks.disable() }
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
 * Closes every non-null context in order, even if one of them fails, and rethrows the first failure.
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
