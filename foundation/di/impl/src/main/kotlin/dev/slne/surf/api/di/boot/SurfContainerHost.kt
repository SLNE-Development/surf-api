package dev.slne.surf.api.di.boot

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.internal.BeanContainerImpl
import dev.slne.surf.api.di.internal.BeanMetadataLoader
import dev.slne.surf.api.di.internal.ConditionEvaluator
import dev.slne.surf.api.di.internal.Decision
import dev.slne.surf.api.di.internal.ReflectiveBeanInstantiator
import dev.slne.surf.api.di.internal.providers
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Builds and keeps the containers of one process.
 *
 * This is the only part of the container a platform calls. It turns an owner into a container by
 * doing the three things that have to happen in order — read the metadata off that owner's
 * classloader, drop the beans whose conditions do not hold, build and start what is left — and it
 * remembers the result, so a platform can ask for an application's container again later without
 * rebuilding it.
 *
 * There is exactly one root, owned by surf-api itself, and one child per application. The root has
 * to be fully started before the first child is built: a child's beans may hold the root's, and a
 * half-started root would hand out a connection nobody has opened yet. [startChild] enforces that
 * rather than trusting the caller to get the order right.
 *
 * Calls are expected to come from the platform's own load sequence, which is single-threaded on
 * every platform surf-api supports. The owner map is concurrent so a later read from another thread
 * is safe, but two simultaneous [startChild] calls for the same owner are not something this class
 * tries to make meaningful.
 */
@InternalSurfApi
class SurfContainerHost(private val platform: ContainerPlatform) {

    private val containers = ConcurrentHashMap<Any, BeanContainerImpl>()

    /**
     * The owners in the order they were started.
     *
     * [containers] answers lookups and [order] answers "which came last", because a hash map has
     * no order to shut down in and sibling applications can be started in any sequence.
     */
    private val order = CopyOnWriteArrayList<Any>()

    @Volatile
    private var rootImpl: BeanContainerImpl? = null

    /** The process-wide container, or `null` until [startRoot] has returned. */
    val root: BeanContainer? get() = rootImpl

    /** The container for [owner], or `null` if it has none. */
    fun containerOf(owner: Any): BeanContainer? = containers[owner]

    /**
     * Builds the root container and runs it through `load` and `enable`.
     *
     * @param owner surf-api itself — whatever the platform uses to identify it, since that is what
     * [ContainerPlatform.classLoader] will be asked about
     * @throws IllegalStateException if a root already exists, or if the root's beans cannot be built
     */
    suspend fun startRoot(owner: Any): BeanContainer {
        check(rootImpl == null) {
            "A root container already exists, owned by ${rootImpl?.owner}. There is one root per " +
                    "process; an application gets a child with startChild."
        }

        // Published only once `start` has returned. A root whose beans failed to build must not be
        // reachable: it would hand out a connection nobody opened, it could never be retried
        // because a root would already exist, and children would be built on top of it.
        val container = build(owner, parent = null)
        container.start()

        rootImpl = container
        containers[owner] = container
        order += owner

        return container
    }

    /**
     * Builds an application's container below the root and runs it through `load` and `enable`.
     *
     * @param owner the application — a plugin instance, a microservice
     * @throws IllegalStateException if the root has not been started, if [owner] already has a
     * container, or if the application's beans cannot be built
     */
    suspend fun startChild(owner: Any): BeanContainer {
        val parent = checkNotNull(rootImpl) {
            "The container for $owner cannot be built before the root container exists. Call " +
                    "startRoot from the platform's own load phase, which runs before any " +
                    "application is loaded."
        }

        check(!containers.containsKey(owner)) {
            "$owner already has a container. Ask for it with containerOf instead of building a " +
                    "second one, which would give the application two instances of every bean."
        }

        // Published after `start`, for the same reason as the root: a container whose beans failed
        // to build would otherwise be handed out by containerOf, and a retry would be refused.
        val container = build(owner, parent)
        container.start()

        containers[owner] = container
        order += owner

        return container
    }

    /**
     * Stops [owner]'s container and forgets it, so the same owner could be started again.
     *
     * Shared beans are left running: they belong to the root and are shut down with it. Failures
     * during teardown are logged through the platform and do not stop the rest.
     *
     * @throws IllegalStateException if [owner] is the root and a child is still running. The
     * ordering rule holds in both directions: a child's bean may hold one of the root's, so
     * stopping the root first would close a connection while something still writes to it, and
     * would leave every child pointing at a stopped parent.
     */
    suspend fun stop(owner: Any) {
        if (owner == rootImpl?.owner) {
            val running = order.filter { it != owner }
            check(running.isEmpty()) {
                "The root container cannot be stopped while ${running.size} application(s) are " +
                        "still running: ${running.joinToString()}. Stop them first, or call " +
                        "stopAll, which does it in the right order."
            }
        }

        val container = containers.remove(owner) ?: return
        order -= owner
        if (container === rootImpl) rootImpl = null

        val logger = platform.logger(owner)
        container.stop { id, failure -> logger.error("$id failed to shut down.", failure) }
    }

    /**
     * Stops every child, youngest first, and then the root.
     *
     * Children go first because a child's bean may hold one of the root's; the reverse order would
     * close a connection while something still writes to it. Among siblings the youngest goes
     * first, which is why [order] exists — a hash map's iteration order would be arbitrary, and the
     * last application started is the one most likely to hold the others.
     */
    suspend fun stopAll() {
        val rootOwner = rootImpl?.owner

        order.asReversed().toList()
            .filter { it != rootOwner }
            .forEach { stop(it) }

        rootOwner?.let { stop(it) }
    }

    /**
     * Reads [owner]'s metadata, evaluates the conditions and returns the container for what is left.
     *
     * The container is built after the conditions rather than before, so a rejected bean is never
     * part of the graph and cannot be resolved by accident. Custom conditions still need a container
     * to ask questions of, which is what [LateContainer] is for: it forwards to the container being
     * built, so a condition sees the real thing and not a throwaway built over unfiltered
     * definitions.
     */
    private suspend fun build(owner: Any, parent: BeanContainerImpl?): BeanContainerImpl {
        val declared = BeanMetadataLoader(platform).load(owner).beans
        val handle = LateContainer()

        val evaluator = ConditionEvaluator(
            platform = platform,
            owner = owner,
            container = handle,
            propertyReader = { _, key, file -> platform.property(owner, key, file) },
        )

        val accepted = accept(declared, parent, evaluator, platform.logger(owner)::debug)

        val container = BeanContainerImpl(
            owner = owner,
            definitions = accepted,
            parentImpl = parent,
            instantiator = ReflectiveBeanInstantiator(platform.classLoader(owner)),
        )

        handle.delegate = container

        return container
    }

    /**
     * Keeps the definitions whose conditions hold, reporting each rejection.
     *
     * `@ConditionalOnMissingBean` asks whether something *else* provides a type, so the answer is
     * computed over everything declared here plus everything the parents provide — not over what
     * has been accepted so far. Over the accepted set the answer would depend on the order the
     * metadata files happened to arrive in, and two runs of the same build could disagree.
     */
    private suspend fun accept(
        declared: List<BeanDefinition>,
        parent: BeanContainerImpl?,
        evaluator: ConditionEvaluator,
        report: (String) -> Unit,
    ): List<BeanDefinition> {
        val providersByType = buildMap<String, MutableSet<String>> {
            declared.forEach { definition ->
                definition.providers().forEach { provider ->
                    provider.provides.forEach { type ->
                        getOrPut(type) { mutableSetOf() }.add(provider.id)
                    }
                }
            }
        }

        return declared.filter { definition ->
            val ownIds = definition.providers().map { it.id }.toSet()

            val decision = evaluator.evaluate(definition) { type ->
                providersByType[type].orEmpty().any { it !in ownIds } ||
                        parent?.provides(type) == true
            }

            when (decision) {
                is Decision.Accepted -> true
                is Decision.Rejected -> {
                    report("Skipping ${definition.className}: ${decision.reason}")
                    false
                }
            }
        }
    }
}

/**
 * A container that forwards to one built later.
 *
 * Only here to break the order between the condition evaluator and the container: the evaluator
 * needs something to hand to a custom condition, and the container needs the evaluator's verdict
 * before it exists. Every member throws until [delegate] is set, which is the honest failure for a
 * condition that tries to resolve a bean while the container is still being decided.
 */
private class LateContainer : BeanContainer {

    lateinit var delegate: BeanContainer

    private val target: BeanContainer
        get() = if (::delegate.isInitialized) delegate else error(
            "A condition asked the container for a bean while the container was still being " +
                    "built. A condition decides whether a bean exists, so it cannot depend on one; " +
                    "ask the platform instead."
        )

    override val parent: BeanContainer? get() = target.parent
    override val owner: Any get() = target.owner

    override fun <T : Any> beanOrNull(type: KClass<T>, qualifier: String?): T? =
        target.beanOrNull(type, qualifier)

    override fun <T : Any> bean(type: KClass<T>, qualifier: String?): T = target.bean(type, qualifier)

    override fun <T : Any> beansOfType(type: KClass<T>): List<T> = target.beansOfType(type)

    override fun <T : Any> lazyBean(type: KClass<T>, qualifier: String?): Lazy<T> =
        target.lazyBean(type, qualifier)
}
