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

/**
 * Creates holders that register with PlaceholderAPI when it is installed, and holders that do
 * nothing otherwise.
 *
 * No PlaceholderAPI type is loaded unless PlaceholderAPI is installed.
 */
internal object PlaceholderApiHolders : ExpansionHolderFactory {

    private val isPlaceholderApiInstalled by lazy {
        runCatching { Class.forName("me.clip.placeholderapi.PlaceholderAPI") }.isSuccess
    }

    override fun create(definition: ExpansionDefinition): ExpansionHolder {
        return if (isPlaceholderApiInstalled) PlaceholderApiExpansion(definition) else NoopHolder
    }

    private object NoopHolder : ExpansionHolder {
        override fun attach() {}

        override fun detach() {}
    }
}
