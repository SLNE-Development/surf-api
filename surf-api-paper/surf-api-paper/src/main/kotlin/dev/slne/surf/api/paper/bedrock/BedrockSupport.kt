package dev.slne.surf.api.paper.bedrock

import org.bukkit.Bukkit

/**
 * Checks whether Floodgate, which is required for the Bedrock API, is installed.
 *
 * Floodgate is an optional dependency, so every entry point of the Bedrock API
 * checks this before touching any Floodgate class.
 */
object BedrockSupport {
    const val FLOODGATE_PLUGIN_NAME = "floodgate"

    /**
     * `true` if Floodgate is installed and enabled.
     */
    val isAvailable: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled(FLOODGATE_PLUGIN_NAME)

    /**
     * Throws an [IllegalStateException] if Floodgate is not installed.
     */
    fun requireAvailable() {
        check(isAvailable) { "Floodgate ($FLOODGATE_PLUGIN_NAME) is not installed or enabled" }
    }
}

/**
 * Runs [block] if Floodgate is installed, otherwise returns [default].
 */
internal inline fun <T> ifBedrockAvailable(default: T, block: () -> T): T =
    if (BedrockSupport.isAvailable) block() else default
