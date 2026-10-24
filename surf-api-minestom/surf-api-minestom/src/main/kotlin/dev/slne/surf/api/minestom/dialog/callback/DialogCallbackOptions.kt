package dev.slne.surf.api.minestom.dialog.callback

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * How long a dialog callback stays valid and how often it may run.
 *
 * @property uses how often the callback may run, or [UNLIMITED_USES]
 * @property lifetime how long the callback stays registered after it was created
 */
data class DialogCallbackOptions(
    val uses: Int = ONE_USE,
    val lifetime: Duration = DEFAULT_LIFETIME,
) {
    init {
        require(uses == UNLIMITED_USES || uses > 0) {
            "A dialog callback must allow at least one use"
        }
        require(lifetime.isPositive()) { "A dialog callback lifetime must be positive" }
    }

    companion object {
        const val ONE_USE = 1
        const val UNLIMITED_USES = -1

        val DEFAULT_LIFETIME: Duration = 12.hours

        val DEFAULT = DialogCallbackOptions()
    }
}
