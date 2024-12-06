package dev.slne.surf.surfapi.bukkit.api.event

import dev.slne.surf.surfapi.bukkit.api.extensions.pluginManager
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass


/**
 * Shortcut for unregistering all events in this listener.
 */
fun Listener.unregister() = HandlerList.unregisterAll(this)

fun <T : Event> Listener.register(
    plugin: Plugin,
    eventClass: KClass<T>,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    executor: (Listener, Event) -> Unit,
) = pluginManager.registerEvent(eventClass.java, this, priority, executor, plugin, ignoreCancelled)


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
) = register(
    JavaPlugin.getProvidingPlugin(this::class.java),
    T::class,
    priority,
    ignoreCancelled,
    executor
)


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

@Suppress("UNCHECKED_CAST")
fun <T : Event> SingleListener<T>.register(
    plugin: Plugin,
    eventClass: KClass<T>,
) {
    pluginManager.registerEvent(
        eventClass.java,
        this,
        priority,
        { _, event -> (event as? T)?.onEvent() },
        plugin,
        ignoreCancelled
    )
}

/**
 * Registers the [SingleListener] with its
 * event listener.
 */
inline fun <reified T : Event> SingleListener<T>.register() =
    register(JavaPlugin.getProvidingPlugin(this::class.java), T::class)

fun Listener.register(plugin: Plugin) = pluginManager.registerEvents(this, plugin)

@Suppress("NOTHING_TO_INLINE")
inline fun Listener.register() = register(JavaPlugin.getProvidingPlugin(this::class.java))

fun <T : Event> listen(
    plugin: Plugin,
    eventClass: KClass<T>,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    register: Boolean = true,
    onEvent: T.() -> Unit,
): SingleListener<T> {
    val listener = object : SingleListener<T>(priority, ignoreCancelled) {
        override fun T.onEvent() {
            onEvent()
        }
    }
    if (register) listener.register(plugin, eventClass)
    return listener
}

/**
 * @param T the type of event to listen to
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 * @param register if the event should be registered immediately
 * @param onEvent the event callback
 */
inline fun <reified T : Event> Any.listen(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    register: Boolean = true,
    noinline onEvent: T.() -> Unit,
): SingleListener<T> = listen(
    JavaPlugin.getProvidingPlugin(this::class.java),
    T::class,
    priority,
    ignoreCancelled,
    register,
    onEvent
)
