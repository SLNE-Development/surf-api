package dev.slne.surf.api.paper.display.element

import dev.slne.surf.api.paper.display.behavior.*
import dev.slne.surf.api.paper.display.style.Style

class Rect(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0)

/**
 * Base class for all UI elements in the display system.
 *
 * Elements form a tree structure (like HTML DOM). Each element has:
 * - A [style] for visual properties (CSS-like)
 * - A list of [children] elements
 * - A list of [behaviors] for interactivity
 */
abstract class Element {
    /** CSS-like style properties for this element. */
    val style = Style()

    /** Child elements. */
    val children = mutableListOf<Element>()

    /** Attached behaviors for interactivity. */
    val behaviors = mutableListOf<Behavior>()

    /** Computed layout bounds (set by the renderer). */
    var bounds = Rect()

    /** Current interaction phase. */
    var phase: ElementPhase = ElementPhase.DEFAULT
        internal set

    /** Configure the style using a DSL block. */
    fun style(block: Style.() -> Unit) {
        style.block()
    }

    // --- Behavior DSL ---

    /** Attach a behavior to this element. */
    fun <T : Behavior> behavior(behavior: T): T {
        behaviors.add(behavior)
        return behavior
    }

    /** Make this element respond to click events. */
    fun clickable(
        onClick: (InteractionContext) -> Unit = {},
        onRightClick: (InteractionContext) -> Unit = {},
    ) {
        behavior(Clickable(onClick, onRightClick))
    }

    /** Make this element respond to hover events. */
    fun hoverable(
        onEnter: (InteractionContext) -> Unit = {},
        onExit: (InteractionContext) -> Unit = {},
    ) {
        behavior(Hoverable(onEnter, onExit))
    }

    /** Make this element draggable. */
    fun draggable(
        onDragStart: (InteractionContext) -> Unit = {},
        onDrag: (InteractionContext) -> Unit = {},
        onDragEnd: (InteractionContext) -> Unit = {},
    ) {
        behavior(Draggable(onDragStart, onDrag, onDragEnd))
    }

    /** Make this element respond to scroll events. */
    fun scrollable(
        onScroll: (InteractionContext, Int) -> Unit = { _, _ -> },
    ) {
        behavior(Scrollable(onScroll))
    }

    /** Attach a tooltip to this element. */
    fun tooltip(text: String) {
        behavior(TooltipBehavior(text))
    }

    /** Shorthand: add a click handler. */
    fun onClick(handler: (InteractionContext) -> Unit) {
        clickable(onClick = handler)
    }

    /** Shorthand: add a right-click handler. */
    fun onRightClick(handler: (InteractionContext) -> Unit) {
        clickable(onRightClick = handler)
    }

    /** Find all behaviors of a specific type. */
    inline fun <reified T : Behavior> findBehaviors(): List<T> =
        behaviors.filterIsInstance<T>()

    /** Check whether the element has a specific behavior type. */
    inline fun <reified T : Behavior> hasBehavior(): Boolean =
        behaviors.any { it is T }
}
