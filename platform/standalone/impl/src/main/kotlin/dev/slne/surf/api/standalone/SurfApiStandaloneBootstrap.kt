package dev.slne.surf.api.standalone

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.core.server.spring.SurfEventHandlerBeans
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.SurfRootContext
import dev.slne.surf.api.spring.context.surfCoroutineScope
import dev.slne.surf.api.standalone.impl.SurfStandaloneInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Runs surf-api in a plain JVM process without a Minecraft server.
 *
 * [bootstrap] creates surf-api's root Spring context, after which the caller runs [enable]; [adopt]
 * instead makes a caller-owned context the root, whose lifecycle runs the hooks. [shutdown]
 * releases the process-wide state. At most one root context exists at a time, and a process that
 * has shut down can bootstrap again.
 */
object SurfApiStandaloneBootstrap {
    private val shutdown = AtomicBoolean(false)

    /**
     * surf-api's root Spring context for this process, or `null` while it is not running.
     *
     * One root and no plugin contexts: a standalone process has a single class loader. An
     * application passed to [bootstrap] lives in this context too.
     */
    @Volatile
    private var root: SurfRootContext? = null

    /**
     * The root context's beans that [enable] registered as event bus listeners; [shutdown]
     * unregisters them.
     */
    private val surfEventListeners = mutableListOf<Any>()

    /**
     * Builds surf-api's root context and runs its load hooks.
     *
     * With an [application], the root context also hosts the application's components,
     * contributions and phase listeners, as a plugin context would; its packages must not overlap
     * surf-api's. surf-api's static entry points can be used while the context refreshes, so that
     * beans such as config handles work during the refresh.
     */
    suspend fun bootstrap(application: PluginDescriptor? = null) {
        check(root == null) { "surf-api is already bootstrapped" }

        shutdown.set(false)

        preparePacketEvents()

        val classLoader = SurfApiStandaloneBootstrap::class.java.classLoader
        val root = try {
            SurfRootContext.create(
                classLoader,
                surfCoroutineScope("surf-api", classLoader, Dispatchers.Default),
                application = application,
                configure = SurfContexts::install,
            )
        } catch (exception: Throwable) {
            SurfContexts.uninstall()
            throw exception
        }

        this.root = root
        load(root)
    }

    /**
     * Makes [context], which the caller refreshes and closes, surf-api's root context that also
     * hosts [application].
     *
     * For a Spring Boot application: call it from an `ApplicationContextInitializer`, before the
     * refresh. The application's components come from the caller's own scan; surf-api's package is
     * scanned and the contributors contribute as [bootstrap] describes. surf-api's static entry
     * points work from the refresh on. The context gets a `SmartLifecycle` bean in
     * [LIFECYCLE_PHASE] that runs the load and enable hooks when the context starts and [shutdown]
     * when it stops; a failing start shuts surf-api down before the failure propagates. A refresh
     * that fails before that bean starts undoes the adoption when the context destroys its beans,
     * so surf-api can be adopted again.
     *
     * The initializer needs no particular order among the other initializers: the NATS integration
     * reads the adopted root when the context refreshes, not when the initializers run. It must
     * adopt the context before the refresh starts.
     *
     * The microservice runtime does the rest for its applications; another Spring Boot program
     * supplies it itself:
     * - the property `surf.config-directory`
     *   ([SurfProperties.CONFIG_DIRECTORY][dev.slne.surf.api.spring.SurfProperties.CONFIG_DIRECTORY]),
     *   from which NATS and the database read their configuration;
     * - [shutdown] when Spring Boot reports a failed start (`ApplicationFailedEvent`), because a
     *   later initializer can fail before the context refreshes and destroys its beans;
     * - its logging policy: `spring-boot` turns off log4j's own shutdown hook, so the program shuts
     *   log4j down itself once the context has closed.
     *
     * @throws IllegalStateException if surf-api is already bootstrapped
     * @throws IllegalArgumentException if [application]'s packages overlap surf-api's
     */
    fun adopt(context: AnnotationConfigApplicationContext, application: PluginDescriptor) {
        check(root == null) { "surf-api is already bootstrapped" }

        shutdown.set(false)
        preparePacketEvents()

        val classLoader = SurfApiStandaloneBootstrap::class.java.classLoader
        val scope = surfCoroutineScope("surf-api", classLoader, Dispatchers.Default)
        val adopted = try {
            SurfRootContext.prepare(context, classLoader, scope, application)
        } catch (exception: Throwable) {
            scope.cancel("surf-api's root context could not be prepared")
            destroyPacketEvents()
            throw exception
        }
        root = adopted
        SurfContexts.install(context)

        context.defaultListableBeanFactory.registerDisposableBean(ADOPTION_GUARD_BEAN) {
            abandon(adopted)
        }
        context.registerBean<SurfStandaloneLifecycle>(LIFECYCLE_BEAN) { SurfStandaloneLifecycle() }
    }

    internal suspend fun loadAdopted() {
        load(checkNotNull(root) { "surf-api has not been adopted" })
    }

    private suspend fun load(root: SurfRootContext) {
        root.load()
        SurfStandaloneInstance.bootstrap()
    }

    /**
     * The `SmartLifecycle` phase in which an adopted context runs surf-api's hooks; beans that need
     * surf-api's enabled state start in a later phase and stop before it.
     */
    const val LIFECYCLE_PHASE = 0

    private const val LIFECYCLE_BEAN = "surfStandaloneLifecycle"
    private const val ADOPTION_GUARD_BEAN = "surfStandaloneAdoptionGuard"
    private const val LOG4J_LOG_MANAGER = "org.apache.logging.log4j.LogManager"

    /**
     * Registers the root context's `@SurfEventHandler` beans with the event bus, runs the root
     * context's enable hooks, then surf-api's own load and enable phases.
     *
     * With [registerShutdownHook], a JVM shutdown hook calls [shutdown] and then shuts log4j down,
     * if the process uses it: `spring-boot`, which surf-api ships, turns off log4j's own shutdown
     * hook. An adopted context calls this from its lifecycle bean without the hook.
     *
     * @throws IllegalStateException if surf-api has not been bootstrapped or adopted
     */
    suspend fun enable(registerShutdownHook: Boolean = true) {
        val root = checkNotNull(root) { "surf-api has not been bootstrapped" }

        SurfEventHandlerBeans.register(listOf(root.context), surfEventListeners)
        root.enable()

        SurfStandaloneInstance.onLoad()
        SurfStandaloneInstance.onEnable()

        if (registerShutdownHook) {
            val hook = thread(start = false) {
                try {
                    runBlocking { shutdown() }
                } finally {
                    shutdownLog4j()
                }
            }
            Runtime.getRuntime().addShutdownHook(hook)
        }
    }

    /**
     * Shuts log4j down, which flushes its asynchronous appenders. Does nothing without log4j on the
     * class path; log4j is reached reflectively, so surf-api needs no dependency on it.
     */
    private fun shutdownLog4j() {
        val logManager = try {
            Class.forName(LOG4J_LOG_MANAGER, false, javaClass.classLoader)
        } catch (_: ClassNotFoundException) {
            return
        }

        logManager.getMethod("shutdown").invoke(null)
    }

    /**
     * Disables surf-api and releases its process-wide state: the event bus listeners [enable]
     * registered, the root context, the installed [SurfContexts] and PacketEvents.
     *
     * Idempotent until the next [bootstrap] or [adopt]. The state is released even if a step fails,
     * and the disable hooks run only while the root context is active. The listeners are
     * unregistered after the disable hooks and before the root context closes.
     */
    suspend fun shutdown() {
        if (shutdown.getAndSet(true)) {
            return
        }

        try {
            SurfStandaloneInstance.onDisable()

            root?.let { root ->
                try {
                    if (root.context.isActive) {
                        root.disable()
                    }
                } finally {
                    try {
                        unregisterSurfEventListeners()
                    } finally {
                        root.close()
                    }
                }
            }
        } finally {
            release()
        }
    }

    /**
     * Undoes [adopt] for [adopted], unless [shutdown] ran already. Runs while the context destroys
     * its beans, so it runs no hooks: a context that gets here without the stop of its lifecycle
     * bean never started surf-api.
     */
    private fun abandon(adopted: SurfRootContext) {
        if (root !== adopted) return

        shutdown.set(true)

        try {
            adopted.close()
        } finally {
            release()
        }
    }

    private fun unregisterSurfEventListeners() {
        val listeners = surfEventListeners.toList()
        surfEventListeners.clear()
        SurfEventHandlerBeans.unregister(listeners)
    }

    private fun release() {
        root = null

        try {
            SurfContexts.uninstall()
        } finally {
            destroyPacketEvents()
        }
    }

    private fun preparePacketEvents() {
        PacketEvents.setAPI(NoopPacketEvents())
        packetEvents.load()
        packetEvents.init()
    }

    private fun destroyPacketEvents() {
        packetEvents.terminate()
    }
}