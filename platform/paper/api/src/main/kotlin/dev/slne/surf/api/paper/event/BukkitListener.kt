package dev.slne.surf.api.paper.event

import dev.slne.surf.api.spring.ManualListenerRegistration
import org.bukkit.event.Listener
import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

/**
 * Marks a component whose role is to handle Bukkit events.
 *
 * The annotated class must implement [Listener]. surf-api registers it, like every other
 * [Listener] bean, in the enable phase of the context that holds it, before the `onEnable` hooks
 * run, and Paper unregisters it when the plugin is disabled. [ManualListenerRegistration] excludes
 * a listener from that registration.
 *
 * ```kotlin
 * @BukkitListener
 * class JoinListener(private val greetings: GreetingService) : Listener {
 *
 *     @EventHandler
 *     fun onJoin(event: PlayerJoinEvent) {
 *         greetings.greet(event.player)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class BukkitListener(
    @get:AliasFor(annotation = Component::class)
    val value: String = "",
)
