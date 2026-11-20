package dev.slne.surf.api.core.server.spring

import dev.slne.surf.api.event.SurfEventBus
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.spring.util.FailureCollector
import org.springframework.context.ConfigurableApplicationContext

/**
 * Registers the beans that declare `@SurfEventHandler` methods as listeners of the [SurfEventBus]:
 * those of a plugin's contexts, or of the root context that hosts a standalone application.
 *
 * Only the beans defined in each context itself are considered, never those of its ancestors.
 */
object SurfEventHandlerBeans {

    /**
     * Registers the listener beans of [contexts] and appends each one to [registered] right after
     * it is registered.
     *
     * [registered] therefore holds every registered listener even when a later registration
     * throws, so that [unregister] can undo a partial registration.
     */
    fun register(contexts: List<ConfigurableApplicationContext>, registered: MutableList<Any>) {
        val listeners = beansDeclaringMethodsAnnotated(contexts, SurfEventHandler::class.java)
        if (listeners.isEmpty()) return

        val eventBus = SurfEventBus.INSTANCE
        for (listener in listeners) {
            eventBus.registerListeners(listener)
            registered += listener
        }
    }

    /**
     * Unregisters [listeners] from the [SurfEventBus].
     *
     * Every listener is unregistered even when one of them fails; the first failure is thrown
     * afterwards, with the later ones suppressed.
     */
    fun unregister(listeners: List<Any>) {
        if (listeners.isEmpty()) return

        val eventBus = SurfEventBus.INSTANCE
        val failures = FailureCollector()
        for (listener in listeners) {
            try {
                eventBus.unregisterListeners(listener)
            } catch (exception: Exception) {
                failures.add(exception)
            }
        }
        failures.throwIfAny()
    }
}
