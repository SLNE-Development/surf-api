package dev.slne.surf.api.velocity.event

import com.velocitypowered.api.event.Subscribe
import dev.slne.surf.api.spring.ManualListenerRegistration
import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

/**
 * Marks a component whose role is to handle Velocity events.
 *
 * The annotated class must declare at least one [Subscribe] method. surf-api registers it, like
 * every other bean with [Subscribe] methods, in the enable phase of the context that holds it,
 * before the `onEnable` hooks run, and unregisters it when the plugin is disabled. Velocity fires
 * `ProxyInitializeEvent` while the plugins enable, so such a bean never receives that event.
 * [ManualListenerRegistration] excludes a listener from the registration.
 *
 * ```kotlin
 * @VelocityListener
 * class LoginListener(private val bans: BanService) {
 *
 *     @Subscribe
 *     fun onLogin(event: LoginEvent) {
 *         bans.check(event)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class VelocityListener(
    @get:AliasFor(annotation = Component::class)
    val value: String = "",
)
