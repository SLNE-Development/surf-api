package dev.slne.surf.api.config

/**
 * Gives access to one configuration file of a plugin, declared by a [SurfConfig] class.
 *
 * Beans inject `ConfigHandle<C>` rather than `C`, so they see the current value after a [reload]
 * or an edit instead of the one loaded at start.
 *
 * Thread-safe: [reload], [edit], [update] and [save] run one at a time. [value] returns the instance
 * the last load, [reload] or [update] installed; [edit] changes that instance in place, so a reader
 * may observe the change while it happens.
 */
interface ConfigHandle<C : Any> {

    /**
     * The current configuration.
     */
    val value: C

    /**
     * Reads the file again, applies pending migrations and returns the new value.
     *
     * Keeps the current value if the file has no configuration in it.
     *
     * @throws java.io.UncheckedIOException if the file cannot be read or does not match [C]
     */
    fun reload(): C

    /**
     * Changes the current value in place and saves it; for classes with mutable properties.
     */
    fun edit(block: C.() -> Unit)

    /**
     * Replaces the current value with the result of [transform] and saves it; for immutable
     * classes, such as a data class edited with `copy`.
     *
     * @return the new value
     */
    fun update(transform: (C) -> C): C

    /**
     * Writes the current value to the file.
     *
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    fun save()
}
