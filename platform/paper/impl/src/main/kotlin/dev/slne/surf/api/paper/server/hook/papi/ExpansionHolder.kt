package dev.slne.surf.api.paper.server.hook.papi

/**
 * Connects one [ExpansionDefinition] to PlaceholderAPI.
 */
internal interface ExpansionHolder {

    /**
     * Registers the expansion with PlaceholderAPI.
     *
     * @throws IllegalStateException if PlaceholderAPI rejects the expansion
     */
    fun attach()

    /**
     * Unregisters the expansion from PlaceholderAPI.
     */
    fun detach()
}

/**
 * Creates the [ExpansionHolder] of a definition.
 *
 * The root context holds exactly one: [PapiExpansionHolderFactory] while PlaceholderAPI is
 * installed, [NoopExpansionHolderFactory] otherwise.
 */
internal fun interface ExpansionHolderFactory {

    /**
     * Creates the holder of [definition].
     *
     * @param definition the expansion to connect
     * @return the holder, not attached yet
     */
    fun create(definition: ExpansionDefinition): ExpansionHolder
}
