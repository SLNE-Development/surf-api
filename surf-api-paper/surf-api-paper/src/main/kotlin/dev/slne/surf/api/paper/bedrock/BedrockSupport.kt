package dev.slne.surf.api.paper.bedrock

import org.bukkit.Bukkit

/**
 * Checks whether the plugins required for the Bedrock API (Geyser and Floodgate) are installed.
 *
 * Geyser and Floodgate are optional dependencies, so every entry point of the Bedrock API
 * checks this before touching any Geyser or Floodgate class.
 */
object BedrockSupport {
    const val GEYSER_PLUGIN_NAME = "Geyser-Spigot"
    const val FLOODGATE_PLUGIN_NAME = "floodgate"

    /**
     * `true` if Geyser is installed and enabled.
     */
    val isGeyserInstalled: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled(GEYSER_PLUGIN_NAME)

    /**
     * `true` if Floodgate is installed and enabled.
     */
    val isFloodgateInstalled: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled(FLOODGATE_PLUGIN_NAME)

    /**
     * `true` if both Geyser and Floodgate are installed and enabled.
     */
    val isAvailable: Boolean
        get() = isGeyserInstalled && isFloodgateInstalled

    /**
     * Throws an [IllegalStateException] if Geyser or Floodgate is not installed.
     */
    fun requireAvailable() {
        check(isGeyserInstalled) { "Geyser ($GEYSER_PLUGIN_NAME) is not installed or enabled" }
        check(isFloodgateInstalled) { "Floodgate ($FLOODGATE_PLUGIN_NAME) is not installed or enabled" }
    }
}

/**
 * Runs [block] if Geyser and Floodgate are installed, otherwise returns [default].
 */
internal inline fun <T> ifBedrockAvailable(default: T, block: () -> T): T =
    if (BedrockSupport.isAvailable) block() else default
