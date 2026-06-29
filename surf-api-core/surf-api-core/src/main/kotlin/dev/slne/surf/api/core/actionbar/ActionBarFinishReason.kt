package dev.slne.surf.api.core.actionbar

/**
 * Describes why a repeating action bar task finished.
 */
enum class ActionBarFinishReason {
    /**
     * The configured duration elapsed successfully.
     */
    COMPLETED,

    /**
     * The task stopped because the target player is no longer online.
     */
    AUTO_CANCELLED,

    /**
     * The returned job was cancelled before completion.
     */
    CANCELLED,

    /**
     * The task failed with an unexpected exception.
     */
    FAILED
}