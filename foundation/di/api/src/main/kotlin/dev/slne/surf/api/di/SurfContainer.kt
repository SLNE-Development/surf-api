package dev.slne.surf.api.di

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.ServiceLoader

/**
 * How the container is reached from a place that has no constructor.
 *
 * Almost nothing should need this. A bean states what it needs as constructor parameters and the
 * container supplies them, which is the whole point. This exists for the edges where there is no
 * constructor to inject into: a platform entry point the server calls, a `companion object` that
 * publishes a service to the rest of an api module, an extension function.
 *
 * It is also the one place a [ServiceLoader] lookup survives, and for a reason that cannot be
 * designed away: the container cannot resolve itself. Everything else that used to be looked up
 * this way is a bean now.
 *
 * @see surfBean for the form to use in a property, which resolves on first access
 */
@InternalSurfApi
object SurfContainer {

    /**
     * Resolved once, on first use rather than at class initialisation.
     *
     * A platform publishes its access through [ServiceLoader], and that provider only exists once
     * the platform has started. Resolving eagerly here would mean the first class in an api module
     * to be loaded decides whether the container is reachable, which is the behaviour this whole
     * accessor replaces.
     */
    private val access: ContainerAccess by lazy {
        val providers = ServiceLoader
            .load(ContainerAccess::class.java, ContainerAccess::class.java.classLoader)
            .toList()

        when (providers.size) {
            1 -> providers.single()
            0 -> error(
                "No ContainerAccess provider is registered, so there is no container to ask. " +
                        "surf-api publishes one when it starts; if you are seeing this inside a " +
                        "test, build a container directly instead of reaching through this object."
            )

            else -> error(
                "Several ContainerAccess providers are registered: " +
                        providers.joinToString { it::class.java.name } +
                        ". Exactly one platform may publish the container, and more than one " +
                        "usually means an api module was bundled into a plugin jar instead of " +
                        "being compiled against."
            )
        }
    }

    /** The process-wide container. */
    val root: BeanContainer
        get() = access.root ?: error(unreachable())

    /**
     * Why there is no container to answer with.
     *
     * The two cases look the same from here and are not the same mistake. Asking before surf-api
     * has started is an ordering problem in the caller. Asking *while* a container is being built
     * is a cycle: the container is instantiating a bean class, and initialising that class ran a
     * static initialiser that asked for a bean — most often a `companion object : Foo by someBean`
     * in the very interface the bean implements, because that form has to read the bean to store
     * it. Nothing can satisfy that request, so the message names the shape instead of the symptom.
     */
    private fun unreachable(): String {
        val owner = access.starting
            ?: return "The root container has not been started yet. A bean was asked for while " +
                    "surf-api was still loading — take it as a constructor parameter, or as a " +
                    "Lazy, so it is resolved when it is used rather than when the class is loaded."

        return "A bean was asked for while the container for $owner was still being built, which " +
                "nothing can answer: the container is part-way through creating its beans. This " +
                "happens when initialising a bean's class runs a static initialiser that asks " +
                "for a bean — in Kotlin, typically `companion object : Foo by someBean`, which " +
                "has to read the bean to store it, in the same interface the bean implements. " +
                "Hold the bean lazily instead (`val instance by surfBean<Foo>()` inside the " +
                "companion), so the initialiser stores the lookup rather than performing it."
    }

    /** The process-wide container, or `null` while surf-api is still loading. */
    val rootOrNull: BeanContainer? get() = access.root

    /** The container belonging to [owner], or `null` if that application has none. */
    fun of(owner: Any): BeanContainer? = access.of(owner)
}

/**
 * What a platform publishes so [SurfContainer] can find its containers.
 *
 * Implemented once per platform and registered as a [ServiceLoader] provider. Two members, because
 * two questions are being asked: what is the process-wide container, and which container does this
 * application have.
 */
@InternalSurfApi
interface ContainerAccess {

    /** The process-wide container, or `null` before it has started. */
    val root: BeanContainer?

    /** The container for [owner], or `null` if there is none. */
    fun of(owner: Any): BeanContainer?

    /**
     * The application whose container is being built at this moment, or `null`.
     *
     * Read only to explain a failure, which is why it has a default: an implementation that cannot
     * tell loses the better message and nothing else.
     */
    val starting: Any? get() = null
}

/**
 * A property that holds a bean, resolved the first time it is read.
 *
 * The form to use wherever a service used to be looked up at class initialisation:
 *
 * ```
 * private val bridge by surfBean<SurfPaperNmsNbtBridge>()
 * ```
 *
 * Laziness is not a convenience here. A property initialised eagerly resolves when its class is
 * loaded, and the class that happens to be loaded first decides whether the container was ready —
 * which makes the order of unrelated class loads part of the behaviour. Reading it on first access
 * moves that to the point where the value is actually wanted, by which time surf-api has started.
 *
 * @param qualifier the name the bean was given with [Qualifier], or `null` for any
 */
@InternalSurfApi
inline fun <reified T : Any> surfBean(qualifier: String? = null): Lazy<T> =
    lazy { SurfContainer.root.bean(T::class, qualifier) }

/**
 * A property that holds every bean of a type, resolved the first time it is read.
 *
 * Empty when there is none: this is a collection, not a lookup, so nothing being registered is an
 * answer rather than a failure.
 */
@InternalSurfApi
inline fun <reified T : Any> surfBeans(): Lazy<List<T>> =
    lazy { SurfContainer.root.beansOfType(T::class) }
