package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

/**
 * Marks an API as experimental, indicating that the API may contain bugs,
 * is subject to change, or might not be fully stable.
 *
 * APIs annotated with `@ExperimentalVisualizerApi` should be used with caution
 * and may require opting into their usage.
 *
 * Users of this annotation should understand that these features
 * could change in future versions without maintaining backward compatibility.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and may contain bugs or change in the future."
)
annotation class ExperimentalVisualizerApi