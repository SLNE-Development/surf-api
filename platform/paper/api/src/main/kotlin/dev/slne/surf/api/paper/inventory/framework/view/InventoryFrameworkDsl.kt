package dev.slne.surf.api.paper.inventory.framework.view


/**
 * DSL marker annotation for all inventory-framework DSL scopes.
 *
 * All lambdas and class types annotated with `@InventoryFrameworkDSL` participate in the
 * same DSL receiver scope restriction: within a marked lambda, only members of the
 * innermost annotated receiver can be called without qualification.
 *
 * This prevents accidental calls to outer DSL scopes (e.g. calling a [ViewConfigBuilder][me.devnatan.inventoryframework.ViewConfigBuilder]
 * method from inside a [RenderContext][me.devnatan.inventoryframework.context.RenderContext]
 * lambda).
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class InventoryFrameworkDSL
