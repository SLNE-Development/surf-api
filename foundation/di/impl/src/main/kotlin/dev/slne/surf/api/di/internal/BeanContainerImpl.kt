package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.BeanPostProcessor
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.definition.BeanDefinition
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

    /** The container that holds [Scope.SHARED] instances for the whole process. */
    private val root: BeanContainerImpl get() = parentImpl?.root ?: this

    /**
     * Creates every bean, runs the post processors and drives `load` and `enable`.
     *
     * The phases are separate passes in dependency order, which is what makes a two-phase resource
     * work: a connection can be opened in `load` while the beans registering against it are still
     * being set up, and only be frozen in `enable`, once every `load` has run.
     *
     * @throws IllegalStateException if called twice, or if a bean cannot be created
     */
    suspend fun start() {
        check(!started) { "The container for $owner has already been started." }
        started = true

        val ordered = BeanGraph.order(index) { type -> parentImpl?.provides(type) == true }

        // Post processors first and separately: they must see every other bean, so they cannot be
        // processed themselves, and they must exist before the first bean passes through the chain.
        ordered.filter { it.definition.postProcessor }.forEach { provider ->
            val processor = create(provider)
            processors += processor as BeanPostProcessor
        }
        processors.sortBy { it.priorityOf() }

        ordered.filterNot { it.definition.postProcessor }.forEach { provider ->
            if (provider.scope == Scope.PROTOTYPE) return@forEach
            process(provider, create(provider))
        }

        lifecycle.forEach { it.load() }
        lifecycle.forEach { it.enable() }
    }

    /**
     * Drives `disable` in reverse creation order and lets the post processors undo their
     * registrations.
     *
     * Reverse order means a bean is torn down before what it depends on, so a client closes before
     * its connection. A failing `disable` is reported and does not stop the rest: a half-shut-down
     * process is worse than a logged failure.
     */
    suspend fun stop(onFailure: (String, Throwable) -> Unit) {
        if (!started) return

        instances.entries.reversed().forEach { (id, instance) ->
            processors.asReversed().forEach { processor ->
                runCatching { processor.beforeDestruction(instance, id) }
                    .onFailure { onFailure(id, it) }
            }
        }

        lifecycle.asReversed().forEach { component ->
            runCatching { component.disable() }
                .onFailure { onFailure(component::class.qualifiedName ?: "a bean", it) }
        }

        started = false
    }

    /**
     * Runs [bean] through the post processor chain and keeps the caches honest if one replaces it.
     *
     * A processor is allowed to hand back a different object — a wrapper, a proxy — and from then on
     * that object is the bean. Both the instance cache and the lifecycle list have to follow, or a
     * later lookup would return the original while the lifecycle drove the replacement.
     */
    private suspend fun process(provider: BeanProvider, bean: Any): Any {
        var current = bean
        processors.forEach { current = it.afterInitialization(current, provider.id) }

        if (current === bean) return current

        cacheFor(provider)[provider.id] = current
        (bean as? SurfComponent)?.let { lifecycle.remove(it) }
        (current as? SurfComponent)?.let { lifecycle += it }

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
            (bean as? SurfComponent)?.let { lifecycle += it }
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

    /** A factory's own parameters; its declaring bean is handled separately. */
    private fun BeanProvider.argumentPoints() = when (this) {
        is ClassProvider -> definition.ctorParams
        is FactoryProvider -> factory.params
    }

    private fun BeanPostProcessor.priorityOf() =
        index.providers.find { it.definition.className == this::class.java.name }?.priority ?: 0

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
        requireNotNull(beanOrNull(type, qualifier)) {
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
        dev.slne.surf.api.di.definition.InjectionPoint(type = type, qualifier = qualifier)
}
