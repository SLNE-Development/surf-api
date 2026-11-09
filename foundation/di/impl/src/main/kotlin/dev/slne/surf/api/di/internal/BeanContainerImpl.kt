package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.BeanPostProcessor
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.InjectionPoint
import kotlin.reflect.KClass

/**
 * One application's beans.
 *
 * Takes the definitions that already passed their conditions, so this class is only about building
 * and running them: order, create, post-process, drive the lifecycle, answer lookups. Condition
 * evaluation happens before a container exists, which keeps that decision out of here and makes the
 * container testable from definitions alone.
 *
 * Scopes decide where an instance is cached. A [Scope.SINGLETON] bean lives in the container that
 * declared it. A [Scope.SHARED] bean lives in the root container, which is what lets one connection
 * serve every application in the process. A [Scope.PROTOTYPE] bean is never cached, and therefore
 * never takes part in the lifecycle — the container does not keep it long enough to shut it down.
 *
 * A shared bean belongs to the root in every respect, not only in the instance cache: the root's
 * post processors wrap it, the root's lifecycle list shuts it down, and this container's own
 * teardown leaves it alone. Otherwise the first application to stop would disable a bean the others
 * are still using, and the root would go on handing out the disabled instance.
 */
internal class BeanContainerImpl(
    override val owner: Any,
    definitions: List<BeanDefinition>,
    private val parentImpl: BeanContainerImpl? = null,
    private val instantiator: BeanInstantiator,
) : BeanContainer, BeanLookup {

    override val parent: BeanContainer? get() = parentImpl
    override val parentLookup: BeanLookup? get() = parentImpl

    private val index = BeanIndex(definitions)
    private val resolver = DependencyResolver(index, this, ::instanceOf)

    /** Instances this container owns, by provider id. */
    private val instances = LinkedHashMap<String, Any>()

    /** Beans taking part in the lifecycle, in creation order. */
    private val lifecycle = mutableListOf<SurfComponent>()

    /** Post processors seen by every bean this container creates, in priority order. */
    private val processors = mutableListOf<BeanPostProcessor>()

    private var started = false

    private var enabled = false

    /**
     * Shared components this container introduced into an already-running root.
     *
     * The root's own phase passes are over by then, so these have to be driven here. Kept apart
     * from [lifecycle] because the root, not this container, shuts them down again.
     */
    private val adoptedShared = mutableListOf<SurfComponent>()

    /**
     * Provider ids that have already been through the post processor chain.
     *
     * A bean is created either in its own turn in the ordering or earlier, as someone else's
     * dependency, and either way it has to be post-processed exactly once. For a shared bean the
     * set that counts is the root's: the second application to declare it finds the instance
     * already built, and running the chain again would wrap a wrapper and leave the first
     * application holding an object the root no longer knows about.
     */
    private val processed = mutableSetOf<String>()

    /** The container that holds [Scope.SHARED] instances for the whole process. */
    private val root: BeanContainerImpl get() = parentImpl?.root ?: this

    /**
     * Creates every bean, runs the post processors and drives `load`.
     *
     * `enable` is [enable]'s job, deliberately. Every platform surf-api runs on has two phases of
     * its own — Paper's `onLoad` and `onEnable`, Velocity's construction and `ProxyInitializeEvent`
     * — and they exist because the second one is the first moment the rest of the server is there.
     * A container that ran both passes in one call would put every bean's `enable` inside the
     * platform's *load* phase, where another plugin's api is not up yet. So the container's phases
     * line up with the platform's, and an embedder with only one phase calls both in a row.
     *
     * The passes are separate and each is in dependency order, which is what makes a two-phase
     * resource work: a connection can be opened in `load` while the beans registering against it
     * are still being set up, and only be frozen in `enable`, once every `load` has run.
     *
     * @throws IllegalStateException if called twice, if a shared bean depends on something only this
     * container provides, or if a bean cannot be created
     */
    suspend fun start() {
        check(!started) { "The container for $owner has already been started." }
        checkSharedDependencies()
        started = true

        val ordered = BeanGraph.order(index) { type -> parentImpl?.provides(type) == true }

        // Post processors first and separately: they must see every other bean, so they cannot be
        // processed themselves, and they must exist before the first bean passes through the chain.
        // `ordered` already uses priority as its tiebreak within the graph's constraints, so taking
        // the processors in that order is their chain order.
        ordered.filter { it.definition.postProcessor }.forEach { provider ->
            processors += create(provider) as BeanPostProcessor
        }

        ordered.filterNot { it.definition.postProcessor }.forEach { provider ->
            if (provider.scope == Scope.PROTOTYPE) return@forEach
            if (!processedFor(provider).add(provider.id)) return@forEach
            process(provider, create(provider))
        }

        // A shared bean joins a root whose phases have already run, so it gets both of them here,
        // and before this container's own beans, which may already hold it. Both, not just `load`,
        // because the bean is the root's state from now on: it outlives this container and serves
        // every other application in the process, so leaving it half-started until this
        // container's own enable phase would make the root's contents depend on the timing of
        // whichever application happened to introduce it — and on that application remembering to
        // enable at all.
        adoptedShared.forEach { it.load() }
        adoptedShared.forEach { it.enable() }

        lifecycle.forEach { it.load() }
    }

    /**
     * Drives `enable`, in the same order `load` ran.
     *
     * Separate from [start] so the pass lands in the platform's own enable phase; see there. Doing
     * nothing when the container was never started is deliberate: a platform that stops after a
     * failed start still reaches its enable phase, and failing a second time there would bury the
     * failure that mattered.
     *
     * @throws IllegalStateException if called twice
     */
    suspend fun enable() {
        if (!started) return
        check(!enabled) { "The container for $owner has already been enabled." }
        enabled = true

        lifecycle.forEach { it.enable() }
    }

    /**
     * Drives `disable` in reverse creation order and lets the post processors undo their
     * registrations.
     *
     * Reverse order means a bean is torn down before what it depends on, so a client closes before
     * its connection. A failing `disable` is reported and does not stop the rest: a half-shut-down
     * process is worse than a logged failure.
     *
     * Shared beans are not touched. They live in the root's caches and lists, so they are shut down
     * exactly once, when the root stops.
     */
    suspend fun stop(onFailure: (String, Throwable) -> Unit) {
        if (!started) return

        // A post processor never sees another processor, on the way up or on the way down.
        instances.entries.reversed()
            .filterNot { (_, instance) -> processors.any { it === instance } }
            .forEach { (id, instance) ->
                processors.asReversed().forEach { processor ->
                    runCatching { processor.beforeDestruction(instance, id) }
                        .onFailure { onFailure(id, it) }
                }
            }

        lifecycle.asReversed().forEach { component ->
            runCatching { component.disable() }
                .onFailure { onFailure(component::class.qualifiedName ?: "a bean", it) }
        }

        adoptedShared.clear()
        started = false
    }

    /**
     * Refuses a shared bean that could not survive this container.
     *
     * A shared instance outlives the application that declared it, so everything it holds has to
     * outlive it too: either the root provides the dependency, or the dependency is itself shared.
     * A shared bean holding this container's own singleton would keep a dead object reachable for
     * every other application in the process, and nothing downstream could tell.
     *
     * Only required single dependencies are checked. A nullable parameter accepts absence, and a
     * collection parameter deliberately gathers what is there at the time, so neither says anything
     * about lifetime.
     */
    private fun checkSharedDependencies() {
        if (parentImpl == null) return

        index.providers
            .filter { it.scope == Scope.SHARED }
            .forEach { provider ->
                provider.argumentPoints()
                    .filter { !it.optional && it.collection == null }
                    .forEach { point -> checkSharedDependency(provider, point) }
            }
    }

    private fun checkSharedDependency(provider: BeanProvider, point: InjectionPoint) {
        // Asking the index the same question the resolver will ask keeps the check exact: a type
        // this container does not answer for falls through to the parent, and a type it does answer
        // for is the provider that would really be injected, whatever else also provides it.
        val own = runCatching { index.resolveSingle(point) }.getOrNull() ?: return
        if (own.scope == Scope.SHARED) return

        error(
            "${provider.id} is SHARED, so it outlives the container for $owner, but it needs " +
                    "${point.type}, which that container provides as ${own.scope}. Give ${own.id} " +
                    "Scope.SHARED as well, move it into the root, or make ${provider.id} a " +
                    "SINGLETON so that both end at the same time."
        )
    }

    /**
     * Runs [bean] through the post processor chain and keeps the caches honest if one replaces it.
     *
     * A processor is allowed to hand back a different object — a wrapper, a proxy — and from then on
     * that object is the bean. Both the instance cache and the lifecycle list have to follow, or a
     * later lookup would return the original while the lifecycle drove the replacement.
     *
     * A shared bean passes through the root's chain, since the root is where it will live.
     */
    private suspend fun process(provider: BeanProvider, bean: Any): Any {
        var current = bean
        processorsFor(provider).forEach { current = it.afterInitialization(current, provider.id) }

        if (current === bean) return current

        cacheFor(provider)[provider.id] = current
        (bean as? SurfComponent)?.let { lifecycleFor(provider).remove(it) }
        (current as? SurfComponent)?.let { lifecycleFor(provider) += it }

        if (provider.scope == Scope.SHARED && root !== this) {
            (bean as? SurfComponent)?.let { adoptedShared.remove(it) }
            (current as? SurfComponent)?.let { adoptedShared += it }
        }

        return current
    }

    private fun create(provider: BeanProvider): Any {
        val cache = cacheFor(provider)
        cache[provider.id]?.let { return it }

        val declaring = (provider as? FactoryProvider)?.let { instanceOf(ClassProvider(it.definition)) }
        val arguments = provider.argumentPoints().map { resolver.resolve(it, provider.id) }
        val bean = instantiator.create(provider, arguments, declaring)

        if (provider.scope != Scope.PROTOTYPE) {
            cache[provider.id] = bean
            (bean as? SurfComponent)?.let { component ->
                lifecycleFor(provider) += component
                if (provider.scope == Scope.SHARED && root !== this) adoptedShared += component
            }
        }

        return bean
    }

    /**
     * Returns the instance for [provider], creating it if this container has not yet.
     *
     * Creation is recursive: resolving an argument reaches back in here for the bean it needs. The
     * graph has already ruled out a cycle, so the recursion terminates.
     */
    private fun instanceOf(provider: BeanProvider): Any = create(provider)

    private fun cacheFor(provider: BeanProvider) =
        if (provider.scope == Scope.SHARED) root.instances else instances

    private fun lifecycleFor(provider: BeanProvider) =
        if (provider.scope == Scope.SHARED) root.lifecycle else lifecycle

    private fun processorsFor(provider: BeanProvider) =
        if (provider.scope == Scope.SHARED) root.processors else processors

    private fun processedFor(provider: BeanProvider) =
        if (provider.scope == Scope.SHARED) root.processed else processed

    /** A factory's own parameters; its declaring bean is handled separately. */
    private fun BeanProvider.argumentPoints() = when (this) {
        is ClassProvider -> definition.ctorParams
        is FactoryProvider -> factory.params
    }

    override fun resolveByName(type: String, qualifier: String?): Any? =
        index.resolveSingle(pointFor(type, qualifier))?.let(::instanceOf)
            ?: parentImpl?.resolveByName(type, qualifier)

    override fun resolveAllByName(type: String, qualifier: String?): List<Any> =
        index.resolveAll(pointFor(type, qualifier)).map(::instanceOf) +
                parentImpl?.resolveAllByName(type, qualifier).orEmpty()

    override fun provides(type: String): Boolean =
        index.providersOf(type).isNotEmpty() || parentImpl?.provides(type) == true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> beanOrNull(type: KClass<T>, qualifier: String?): T? =
        resolveByName(type.java.name, qualifier) as T?

    override fun <T : Any> bean(type: KClass<T>, qualifier: String?): T =
        checkNotNull(beanOrNull(type, qualifier)) {
            buildString {
                append("No bean provides ").append(type.java.name)
                qualifier?.let { append(" with qualifier '").append(it).append('\'') }
                append(" in the container for ").append(owner)
                append(". Annotate the implementation, publish it with @Bean, or check whether a ")
                append("condition skipped it.")
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> beansOfType(type: KClass<T>): List<T> =
        resolveAllByName(type.java.name, qualifier = null) as List<T>

    override fun <T : Any> lazyBean(type: KClass<T>, qualifier: String?): Lazy<T> =
        lazy { bean(type, qualifier) }

    private fun pointFor(type: String, qualifier: String?) =
        InjectionPoint(type = type, qualifier = qualifier)
}
