package dev.slne.surf.surfapi.bukkit.api.inventory.dsl

/**
 * DSL marker annotation for pane builder blocks.
 *
 * Annotating a type or class with `@PaneMarker` restricts implicit `this`
 * access within pane DSL scopes so that only members of the innermost receiver
 * can be called without qualification, preventing accidental access to outer
 * GUI builder scopes.
 *
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.MenuMarker
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class PaneMarker
