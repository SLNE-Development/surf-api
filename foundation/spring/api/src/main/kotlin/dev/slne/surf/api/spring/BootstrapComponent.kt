package dev.slne.surf.api.spring

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

/**
 * Marks a component that belongs to a plugin's bootstrap context on Paper.
 *
 * A bootstrap context contains the plugin's components that implement [SurfBootstrapLifecycle] and
 * those carrying this annotation, so a bootstrap bean can depend on helpers of its own plugin. The
 * same rules apply to both: no API of a plugin that is not a bootstrap dependency, no Bukkit server
 * API in the constructor. The plugin's full context injects bootstrap components like any other
 * bean. On platforms without a bootstrap phase it is an ordinary component, and the
 * [SurfBootstrapLifecycle] hooks run right before the load hooks.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class BootstrapComponent(
    @get:AliasFor(annotation = Component::class)
    val value: String = "",
)
