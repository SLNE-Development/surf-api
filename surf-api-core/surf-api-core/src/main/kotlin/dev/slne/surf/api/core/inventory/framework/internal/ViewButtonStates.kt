package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Per-viewer state of a stateful inventory button.
 *
 * A session owns the ordered [states] a button can cycle through, the [initial] value it started
 * out with, and the value it currently shows. It is created once per view session (per player) and
 * mutated in place whenever the button is clicked, which is what allows the view to tell on close
 * whether the button was actually used — see [hasChanged].
 *
 * All of the cycling logic lives here so the per-platform button implementations cannot drift
 * apart. This is internal infrastructure — use the platform `toggleButton` / `statefulButton` /
 * `tripleButton` DSL instead.
 *
 * @param T the type identifying one button state
 * @property states the ordered states the button cycles through; must be non-empty and is not
 *   copied, so callers must pass an immutable list
 * @property initial the state the button started out with for this viewer
 */
@InternalSurfApi
class ViewButtonSession<T>(
    val states: List<T>,
    val initial: T,
) {
    /** Index into [states] of the currently shown state. */
    var currentIndex: Int = states.indexOf(initial)
        private set

    init {
        require(states.isNotEmpty()) { "A stateful button must declare at least one state" }
        require(currentIndex >= 0) {
            "The initial value '$initial' is not one of the button's declared states $states"
        }
    }

    /** The currently shown state. */
    val current: T get() = states[currentIndex]

    /**
     * Whether the button is currently showing a state other than the one it started out with.
     *
     * Note that this compares against [initial] rather than counting clicks, so cycling all the way
     * back to the starting state counts as *unchanged*.
     */
    val hasChanged: Boolean get() = current != initial

    /**
     * Moves [step] states forward (or backward for a negative [step]), wrapping around at both
     * ends of [states].
     *
     * @param step how many states to move; `0` and buttons with a single state never move
     * @return `true` if the current state actually changed
     */
    fun advance(step: Int): Boolean {
        if (step == 0 || states.size < 2) return false

        val next = Math.floorMod(currentIndex + step, states.size)
        if (next == currentIndex) return false

        currentIndex = next
        return true
    }

    /**
     * Jumps directly to [value].
     *
     * @param value the state to show; must be one of [states]
     * @return `true` if the current state actually changed
     * @throws IllegalArgumentException if [value] is not one of [states]
     */
    fun select(value: T): Boolean {
        val index = states.indexOf(value)
        require(index >= 0) {
            "The value '$value' is not one of the button's declared states $states"
        }

        if (index == currentIndex) return false

        currentIndex = index
        return true
    }
}
