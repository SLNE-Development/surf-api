package dev.slne.surf.api.di

/**
 * Sees every bean the container creates, right after it has been constructed and injected.
 *
 * This is how a bean gets registered somewhere without anything having to collect it: the processor
 * inspects the bean — its methods, its supertypes, its annotations — and acts on what it finds.
 * Registering message handlers works this way, because what identifies a handler sits on a method,
 * not on the class:
 *
 * ```kotlin
 * @Service
 * class RabbitRegistrar(private val api: Lazy<ServerRabbitMQApi>) : BeanPostProcessor {
 *
 *     override suspend fun afterInitialization(bean: Any, name: String): Any {
 *         if (bean.hasMethodAnnotated<RabbitHandler>()) {
 *             api.value.registerRequestHandler(bean)
 *         }
 *         return bean
 *     }
 * }
 * ```
 *
 * The dependency runs one way only: the handler beans know the connection, the connection knows
 * nothing about them, and the registrar sits beside both. Collecting the handlers into the
 * connection's constructor instead would invert that and make a handler that publishes a cycle.
 *
 * A processor is an ordinary bean, so it is annotated and may take constructor parameters. Take the
 * thing you register into as `Lazy<T>`: a processor that demands its target eagerly would force
 * that bean to be created before the processor can run, which the container cannot promise for
 * every ordering. A processor never sees itself or another processor.
 *
 * [afterInitialization] returns the bean to use from then on. Return the bean unchanged unless the
 * point is to replace it with a wrapper; returning something that does not satisfy the injection
 * points expecting it fails at the first use.
 */
interface BeanPostProcessor {

    /**
     * Called for every bean after construction and injection, before any lifecycle phase.
     *
     * @param bean the fully injected bean
     * @param name the bean's class name, for messages
     * @return the bean to use from here on, normally [bean] itself
     */
    suspend fun afterInitialization(bean: Any, name: String): Any = bean

    /**
     * Called for every bean during shutdown, before its [SurfComponent.disable].
     *
     * Undo here what [afterInitialization] registered, so a bean that goes away is also unregistered.
     */
    suspend fun beforeDestruction(bean: Any, name: String) = Unit
}
