package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

/**
 * DSL marker annotation for all inventory-framework DSL scopes.
 *
 * All lambdas and class types annotated with `@InventoryFramworkDSL` participate in the
 * same DSL receiver scope restriction: within a marked lambda, only members of the
 * innermost annotated receiver can be called without qualification.
 *
 * This prevents accidental calls to outer DSL scopes (e.g. calling a [ViewConfigBuilder]
 * method from inside a [RenderContext][me.devnatan.inventoryframework.context.RenderContext]
 * lambda).
 *
 * Note: the intentional typo "Framwork" (missing 'e') matches the existing annotation name
 * in the codebase and must not be changed.
 *
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.MenuMarker
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class InventoryFramworkDSL
