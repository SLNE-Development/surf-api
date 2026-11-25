package dev.slne.surf.api.spring.context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.metrics.ApplicationStartup
import java.util.*

/**
 * One of surf-api's root contexts, holding surf-api's own beans, together with the driver of their
 * lifecycle hooks.
 *
 * Paper builds two: a root bootstrap context in surf-api's bootstrap phase with surf-api's
 * bootstrap components, and the root context in surf-api's load phase below it with everything
 * else. Other platforms build one. Both scan every class [classLoader] sees under
 * [SURF_API_PACKAGE]. A plugin that shares surf-api's class loader must therefore live outside that
 * package; [SurfContextManager] rejects one that does not.
 *
 * A root context can also host an application in the same context, such as a standalone service:
 * its components are scanned next to surf-api's, every [ContextContributor] found through
 * [ServiceLoader] contributes to it as it would to a plugin context, and its [ContextPhaseListener]
 * beans run around the enable and disable hooks.
 *
 * Like a plugin context, each phase function runs as the startup step of its [StartupPhase],
 * tagged with the owner's name, and the enable phase ends with one info line of the phase
 * durations. On Paper the root bootstrap context and the root context log a line each, starting
 * with `Root bootstrap context of '<owner>'` and `Root context of '<owner>'`.
 */
class SurfRootContext private constructor(
    private val managed: ManagedContext,
    private val classLoader: ClassLoader,
    private val coroutineScope: CoroutineScope?,
    private val owner: String,
    private val startupSubject: String,
    private val bootstrapsOnLoad: Boolean,
    private val ownsContext: Boolean = true,
) {

    /**
     * The context itself.
     */
    val context: AnnotationConfigApplicationContext get() = managed.context

    private val log = LogFactory.getLog(SurfRootContext::class.java)
    private val hookContext = coroutineScope?.let(::hookElements)
        ?: ContextClassLoaderElement(classLoader)

    /**
     * Runs the bootstrap hooks of the context's beans.
     */
    suspend fun bootstrap() {
        withContext(hookContext) {
            managed.measure(StartupPhase.BOOTSTRAP, owner) { managed.hooks.bootstrap() }
        }
    }

    /**
     * Runs the load hooks of the context's beans.
     *
     * A root context that holds the bootstrap components itself, as on every platform but Paper,
     * runs their bootstrap hooks first.
     */
    suspend fun load() {
        withContext(hookContext) {
            if (bootstrapsOnLoad) {
                managed.measure(StartupPhase.BOOTSTRAP, owner) { managed.hooks.bootstrap() }
            }

            managed.measure(StartupPhase.LOAD, owner) { managed.hooks.load() }
        }
    }

    /**
     * Runs the enable hooks of the context's beans, then logs the startup durations at info level.
     */
    suspend fun enable() {
        withContext(hookContext) {
            managed.measure(StartupPhase.ENABLE, owner) {
                managed.hooks.enable()
                context.runAfterEnable()
            }
        }

        log.info(describeStartup(startupSubject, managed.timings))
    }

    /**
     * Runs the disable hooks of the context's beans in reverse order, then cancels the context's
     * coroutine scope; failures of the hooks are logged.
     */
    suspend fun disable() {
        try {
            withContext(hookContext) {
                managed.measure(StartupPhase.DISABLE, owner) {
                    context.runBeforeDisable(owner)
                    managed.hooks.disable()
                }
            }
        } finally {
            coroutineScope?.cancel("surf-api is disabled")
        }
    }

    /**
     * Cancels the context's coroutine scope and closes the context, unless the context was handed
     * to [prepare], whose caller closes it.
     */
    fun close() {
        coroutineScope?.cancel("surf-api's root context is closed")

        if (ownsContext) {
            withContextClassLoader(classLoader) { context.close() }
        }
    }

    companion object {

        /**
         * Builds and refreshes a root context.
         *
         * @param classLoader the loader of surf-api's classes
         * @param coroutineScope surf-api's own coroutine scope, registered as the context's
         * `CoroutineScope` bean and cancelled by [disable] or [close]; `null` for Paper's root
         * bootstrap context, which has none
         * @param bootstrap `true` for Paper's root bootstrap context, which holds only bootstrap
         * components; `false` for a root context, which leaves them out if [parent] is a root
         * bootstrap context
         * @param parent the root bootstrap context, for Paper's root context
         * @param application the application this context hosts besides surf-api, or `null`; its
         * packages must not overlap [SURF_API_PACKAGE] when it shares [classLoader]
         * @param configure registers further beans before the refresh
         * @throws IllegalArgumentException if [application]'s packages overlap surf-api's
         */
        fun create(
            classLoader: ClassLoader,
            coroutineScope: CoroutineScope?,
            bootstrap: Boolean = false,
            parent: SurfRootContext? = null,
            application: PluginDescriptor? = null,
            configure: (AnnotationConfigApplicationContext) -> Unit = {},
        ): SurfRootContext {
            val selection = when {
                bootstrap -> ScanSelection.BOOTSTRAP
                parent != null -> ScanSelection.WITHOUT_BOOTSTRAP
                else -> ScanSelection.ALL
            }
            val displayName = if (bootstrap) "surf root bootstrap context" else "surf root context"

            val managed = try {
                if (application != null) {
                    checkOutsideRootScan(application, classLoader)
                }

                withContextClassLoader(classLoader) {
                    buildManagedContext(
                        OWNER,
                        classLoader,
                        parent?.context,
                        displayName,
                    ) { context ->
                        prepareRoot(context, classLoader, coroutineScope, selection)

                        if (application != null) {
                            hostApplication(context, application)
                        }

                        configure(context)
                    }
                }
            } catch (exception: Throwable) {
                coroutineScope?.cancel("surf-api's root context could not be built")
                throw exception
            }

            val owner = application?.name ?: OWNER
            val startupSubject = if (bootstrap) {
                "Root bootstrap context of '$owner'"
            } else {
                "Root context of '$owner'"
            }

            return SurfRootContext(
                managed,
                classLoader,
                coroutineScope,
                owner,
                startupSubject,
                bootstrapsOnLoad = selection == ScanSelection.ALL,
            )
        }

        /**
         * Prepares [context], which the caller refreshes and closes, as a root context that hosts
         * [application].
         *
         * For a context that another framework creates, such as a Spring Boot application:
         * surf-api's package is scanned and every [ContextContributor] found through
         * [ServiceLoader] contributes, but [application]'s packages are not scanned, because the
         * caller registers the application's components itself. Call it before the refresh. [close]
         * cancels the coroutine scope and leaves the context open.
         *
         * [context] gets surf-api's application startup unless the caller already installed one.
         * Its refresh is the caller's, so it is not measured as a [StartupPhase.BUILD] step.
         *
         * @param classLoader the loader of surf-api's classes
         * @throws IllegalArgumentException if [application]'s packages overlap surf-api's
         */
        fun prepare(
            context: AnnotationConfigApplicationContext,
            classLoader: ClassLoader,
            coroutineScope: CoroutineScope,
            application: PluginDescriptor,
        ): SurfRootContext {
            checkOutsideRootScan(application, classLoader)
            val hooks = LifecycleHookRunner(application.name)
            hooks.install(context.beanFactory)

            if (context.applicationStartup === ApplicationStartup.DEFAULT) {
                context.applicationStartup = ContextStartup.current()
            }

            withContextClassLoader(classLoader) {
                prepareRoot(context, classLoader, coroutineScope, ScanSelection.ALL)
                contribute(context, application)
            }

            val managed = ManagedContext(context, hooks)

            return SurfRootContext(
                managed,
                classLoader,
                coroutineScope,
                application.name,
                "Root context of '${application.name}'",
                bootstrapsOnLoad = true,
                ownsContext = false,
            )
        }

        /**
         * Registers [coroutineScope], if any, and scans surf-api's own components into [context].
         */
        private fun prepareRoot(
            context: AnnotationConfigApplicationContext,
            classLoader: ClassLoader,
            coroutineScope: CoroutineScope?,
            selection: ScanSelection,
        ) {
            if (coroutineScope != null) {
                registerCoroutineScope(context, coroutineScope, primary = false)
            }

            scanComponents(context, classLoader, listOf(SURF_API_PACKAGE), selection)
        }

        /**
         * Scans [application]'s components into [context] and lets the contributors contribute to
         * it.
         */
        private fun hostApplication(
            context: AnnotationConfigApplicationContext,
            application: PluginDescriptor,
        ) {
            scanComponents(
                context,
                application.classLoader,
                application.basePackages,
                ScanSelection.ALL,
                application.source,
            )
            contribute(context, application)
        }

        private fun contribute(
            context: AnnotationConfigApplicationContext,
            application: PluginDescriptor,
        ) {
            val contributors = ServiceLoader.load(
                ContextContributor::class.java,
                application.classLoader,
            )
            for (contributor in contributors) {
                contributor.contribute(context, application)
            }
        }

        /**
         * The package scanned for surf-api's components.
         */
        const val SURF_API_PACKAGE = "dev.slne.surf.api"

        private const val OWNER = "surf-api"
    }
}
