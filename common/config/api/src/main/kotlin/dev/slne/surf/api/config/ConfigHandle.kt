package dev.slne.surf.api.config

/**
 * Gives access to one configuration file.
 *
 * A plugin gets a handle per [SurfConfig] class injected, or reads it statically through
 * [configHandle] and [surfConfig]; any other file is opened with [ConfigHandles.open]. Code keeps
 * the handle rather than the value, so it sees the value a [reload] or [update] installs.
 *
 * A handle loads its file on [load]. [value], [reload], [edit], [update] and [save] load it first
 * if that has not happened yet, so a lazy [SurfConfig] handle reads its file on the first access;
 * [close] never loads. A load that fails propagates its exception and leaves the handle unloaded;
 * the next access tries again.
 *
 * Thread-safe: [load], [reload], [edit], [update], [save] and [close] run one at a time, so
 * concurrent first accesses read the file once. Once loaded, [value] takes no lock and returns the
 * instance the last load, [reload] or [update] installed; [edit] changes that instance in place,
 * so a reader may observe the change while it happens.
 *
 * A closed handle keeps returning its last [value] and rejects every other operation; one closed
 * before it loaded rejects [value] as well. The handles of [SurfConfig] classes close with their
 * plugin's context.
 *
 * @param C the configuration class
 */
interface ConfigHandle<C : Any> : AutoCloseable {

    /**
     * The current configuration; loads the file first if it is not loaded yet.
     *
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the file cannot be loaded
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the file does not
     * match [C]
     * @throws IllegalStateException if the handle was closed before it loaded
     */
    val value: C

    /**
     * Reads, migrates and writes back the file with its defaults if that has not happened yet;
     * otherwise returns the current value.
     *
     * @return the current value
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the file cannot be read,
     * migrated or written
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the file does not
     * match [C]
     * @throws IllegalStateException if the handle is closed
     */
    fun load(): C

    /**
     * Reads the file into a candidate, applies pending migrations to it and installs it once
     * [validate] accepts it.
     *
     * The file is written only when a migration changed it. A file that holds no configuration,
     * such as an emptied one, keeps the current value, logs a warning and is left as it is.
     *
     * On a handle that has not loaded yet, the file is read, migrated and written back with its
     * defaults as by [load], and the result is installed once [validate] accepts it; when
     * [validate] throws, the handle stays unloaded.
     *
     * @param validate checks the candidate; when it throws, the exception propagates and nothing
     * is installed, and on a loaded handle the file stays unchanged as well
     * @return the installed value
     * @throws java.io.UncheckedIOException if the file cannot be read, migrated or written, or does
     * not match [C]
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the handle is not loaded
     * yet and its file cannot be loaded
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the handle is not
     * loaded yet and its file does not match [C]
     * @throws IllegalStateException if the handle is closed
     */
    fun reload(validate: (C) -> Unit = {}): C

    /**
     * Changes the current value in place and saves it; for classes with mutable properties.
     *
     * @param block changes the value it receives
     * @throws java.io.UncheckedIOException if the file cannot be written
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the handle is not loaded
     * yet and its file cannot be loaded
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the handle is not
     * loaded yet and its file does not match [C]
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
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the handle is not loaded
     * yet and its file cannot be loaded
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the handle is not
     * loaded yet and its file does not match [C]
     * @throws IllegalStateException if the handle is closed
     */
    fun update(transform: (C) -> C): C

    /**
     * Writes the current value to the file.
     *
     * @throws java.io.UncheckedIOException if the file cannot be written
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the handle is not loaded
     * yet and its file cannot be loaded
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the handle is not
     * loaded yet and its file does not match [C]
     * @throws IllegalStateException if the handle is closed
     */
    fun save()

    /**
     * Closes the handle; closing it again does nothing.
     */
    override fun close()
}
