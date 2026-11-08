package dev.slne.surf.api.paper.inventory.framework.dsl

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.context.IFContext

/**
 * Opens the given [other] view for every player who currently has this context's view open.
 *
 * A type-safe convenience wrapper around [IFContext.openForEveryone] that accepts a
 * [RootView] instance and resolves its Java class automatically.
 *
 * ```kotlin
 * onViewClick { click ->
 *     click.openForEveryone(broadcastView)
 * }
 * ```
 *
 * @receiver the [IFContext] whose viewers should transition to the new view
 * @param other the [RootView] to open for all current viewers
 */
fun IFContext.openForEveryone(other: RootView) = openForEveryone(other.javaClass)

/**
 * Opens the given [other] view for every player who currently has this context's view open,
 * passing [initialData] as the view's initial state.
 *
 * ```kotlin
 * onViewClick { click ->
 *     click.openForEveryone(resultView, result)
 * }
 * ```
 *
 * @receiver the [IFContext] whose viewers should transition to the new view
 * @param other the [RootView] to open for all current viewers
 * @param initialData arbitrary data to pass as the new view's initial state
 */
fun IFContext.openForEveryone(other: RootView, initialData: Any) =
    openForEveryone(other.javaClass, initialData)