package dev.slne.surf.api.command

/**
 * Registers commands of a plugin with the platform.
 *
 * Every bean implementing it is called once, in the plugin's enable phase, before the plugin's
 * `onEnable` hooks run. It fits commands built with surf-api's command DSL, which registers a
 * command as it builds it; a bean that extends `CommandAPICommand` or `CommandTree` itself needs no
 * registrar, because surf-api registers it directly.
 *
 * ```kotlin
 * @Component
 * class ShopCommands(private val shop: ShopService) : CommandRegistrar {
 *     override fun register() {
 *         commandAPICommand("shop") {
 *             playerExecutorSuspend { player, _ -> shop.open(player) }
 *         }
 *     }
 * }
 * ```
 */
fun interface CommandRegistrar {

    /**
     * Registers the commands.
     */
    fun register()
}
