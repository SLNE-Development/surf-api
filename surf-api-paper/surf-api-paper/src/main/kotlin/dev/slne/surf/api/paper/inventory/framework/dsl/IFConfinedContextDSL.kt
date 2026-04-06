package dev.slne.surf.api.paper.inventory.framework.dsl

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.context.IFConfinedContext

/**
 * Opens the given [view] for the confined player without any initial data.
 *
 * A type-safe convenience wrapper around [IFConfinedContext.openForPlayer] that accepts
 * a [RootView] instance and resolves its Java class automatically.
 *
 * ```kotlin
 * onViewClick { click ->
 *     click.openForPlayer(myOtherView)
 * }
 * ```
 *
 * @receiver the [IFConfinedContext] (a context tied to a specific player)
 * @param view the [RootView] to open for the confined player
 */
fun IFConfinedContext.openForPlayer(view: RootView) = openForPlayer(view.javaClass)

/**
 * Opens the given [view] for the confined player with arbitrary [initialData].
 *
 * A type-safe convenience wrapper around [IFConfinedContext.openForPlayer] that accepts
 * a [RootView] instance and resolves its Java class automatically.
 *
 * ```kotlin
 * onViewClick { click ->
 *     click.openForPlayer(detailView, selectedItem)
 * }
 * ```
 *
 * @receiver the [IFConfinedContext] (a context tied to a specific player)
 * @param view the [RootView] to open for the confined player
 * @param initialData arbitrary data to pass as the view's initial state
 */
fun IFConfinedContext.openForPlayer(view: RootView, initialData: Any) =
    openForPlayer(view.javaClass, initialData)
