package dev.slne.surf.surfapi.bukkit.api.event

import dev.slne.surf.surfapi.bukkit.api.extensions.pluginManager
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin


/**
 * Shortcut for unregistering all events in this listener.
 */
fun Listener.unregister() = HandlerList.unregisterAll(this)

/**
 * Registers the event with a custom event [executor].
 *
 * @param T the type of event
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 * @param executor handles incoming events
 */
inline fun <reified T : Event> Listener.register(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline executor: (Listener, Event) -> Unit,
) {
    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
    pluginManager.registerEvent(T::class.java, this, priority, executor, plugin, ignoreCancelled)
}

/**
 * This class represents a [Listener] with
 * only one event to listen to.
 */
abstract class SingleListener<T : Event>(
    val priority: EventPriority = EventPriority.NORMAL,
    val ignoreCancelled: Boolean = false,
) : Listener {
    abstract fun T.onEvent()
}

/**
 * Registers the [SingleListener] with its
 * event listener.
 */
inline fun <reified T : Event> SingleListener<T>.register() {
    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
    pluginManager.registerEvent(
        T::class.java,
        this,
        priority,
        { _, event -> (event as? T)?.onEvent() },
        plugin,
        ignoreCancelled
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun Listener.register() {
    pluginManager.registerEvents(this, JavaPlugin.getProvidingPlugin(this::class.java))
}

/**
 * @param T the type of event to listen to
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 * @param register if the event should be registered immediately
 * @param onEvent the event callback
 */
inline fun <reified T : Event> listen(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    register: Boolean = true,
    crossinline onEvent: T.() -> Unit,
): SingleListener<T> {
    val listener = object : SingleListener<T>(priority, ignoreCancelled) {
        override fun T.onEvent() {
            onEvent()
        }
    }
    if (register) listener.register()
    return listener
}