package dev.slne.surf.api.config

/**
 * Gives access to one configuration file.
 *
 * A plugin gets a handle per [SurfConfig] class injected, or reads it statically through
 * [configHandle] and [surfConfig]; any other file is opened with [ConfigHandles.open]. Code keeps
 * the handle rather than the value, so it sees the value a [reload] or [update] installs.
 *
 * Thread-safe: [reload], [edit], [update], [save] and [close] run one at a time. [value] returns
 * the instance the last load, [reload] or [update] installed; [edit] changes that instance in
 * place, so a reader may observe the change while it happens.
 *
 * A closed handle keeps returning its last [value] and rejects every other operation. The handles
 * of [SurfConfig] classes close with their plugin's context.
 *
 * @param C the configuration class
 */
interface ConfigHandle<C : Any> : AutoCloseable {

    /**
     * The current configuration.
     */
    val value: C

    /**
     * Reads the file into a candidate, applies pending migrations to it and installs it once
     * [validate] accepts it.
     *
     * The file is written only when a migration changed it. A file that holds no configuration,
     * such as an emptied one, keeps the current value, logs a warning and is left as it is.
     *
     * @param validate checks the candidate; when it throws, the current value and the file stay
     * unchanged and the exception propagates
     * @return the installed value
     * @throws java.io.UncheckedIOException if the file cannot be read, migrated or written, or does
     * not match [C]
     * @throws IllegalStateException if the handle is closed
     */
    fun reload(validate: (C) -> Unit = {}): C

    /**
     * Changes the current value in place and saves it; for classes with mutable properties.
     *
     * @param block changes the value it receives
     * @throws java.io.UncheckedIOException if the file cannot be written
     * @throws IllegalStateException if the handle is closed
     */
    fun edit(block: C.() -> Unit)

    /**
     * Replaces the current value with the result of [transform] and saves it; for immutable
     * classes, such as a data class edited with `copy`.
     *
     * @param transform returns the new value for the current one
     * @return the new value
     * @throws java.io.UncheckedIOException if the file cannot be written
     * @throws IllegalStateException if the handle is closed
     */
    fun update(transform: (C) -> C): C

    /**
     * Writes the current value to the file.
     *
     * @throws java.io.UncheckedIOException if the file cannot be written
     * @throws IllegalStateException if the handle is closed
     */
    fun save()

    /**
     * Closes the handle; closing it again does nothing.
     */
    override fun close()
}
