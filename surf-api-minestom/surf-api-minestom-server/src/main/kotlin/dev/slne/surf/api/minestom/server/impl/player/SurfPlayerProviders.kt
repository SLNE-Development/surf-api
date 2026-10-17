package dev.slne.surf.api.minestom.server.impl.player

import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.server.SurfMinestomPlayerFactory
import net.minestom.server.entity.Player
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.property.ServerProperties

/**
 * Owns Minestom's player provider while the surf api runs.
 *
 * Between [install] and [uninstall] the provider creates every player with the factory of the
 * server, and `ConnectionManagerMixin` rejects any other provider through [checkProvider]. Outside
 * of that window Minestom's setter behaves as usual, but a custom provider that is still installed
 * when the api starts fails [install] instead of being replaced silently. In tests
 * ([ServerProperties.INSIDE_TEST]) it is replaced, since Minestom's test environment installs a
 * provider of its own for every test. Without Mixin nothing is enforced and [install] replaces any
 * provider.
 */
object SurfPlayerProviders {

    private var owned = false
    private var foreignProviderInstalled = false

    /**
     * Installs the provider of [factory] and takes ownership of Minestom's player provider.
     *
     * @param replaceForeignProvider whether a provider installed through Minestom is replaced instead
     * of failing the call
     * @throws IllegalStateException when a provider other than the surf one is installed and
     * [replaceForeignProvider] is `false`
     */
    @Synchronized
    internal fun install(
        factory: SurfMinestomPlayerFactory,
        replaceForeignProvider: Boolean = ServerProperties.INSIDE_TEST.get(),
    ) {
        check(!foreignProviderInstalled || replaceForeignProvider) {
            "ConnectionManager#setPlayerProvider installed a provider before the surf api started; " +
                    "set the player class with surfMinestomServer { playerFactory { connection, profile -> ... } } instead"
        }

        ConnectionManager.setPlayerProvider(SurfPlayerProvider(factory))
        owned = true
    }

    /**
     * Gives up ownership and restores Minestom's default provider.
     */
    @Synchronized
    internal fun uninstall() {
        if (!owned) return

        owned = false
        ConnectionManager.setPlayerProvider(null)
    }

    /**
     * Entry point of `ConnectionManagerMixin`, called before Minestom installs [provider].
     *
     * @throws IllegalStateException when the surf api owns the provider and [provider] is not the
     * surf one
     */
    @JvmStatic
    @Synchronized
    fun checkProvider(provider: PlayerProvider?) {
        val surf = provider is SurfPlayerProvider
        check(!owned || surf) {
            "The surf api creates the players of this server; set the player class with " +
                    "surfMinestomServer { playerFactory { connection, profile -> ... } } instead of " +
                    "ConnectionManager#setPlayerProvider"
        }

        foreignProviderInstalled = provider != null && !surf
    }

    private class SurfPlayerProvider(private val factory: SurfMinestomPlayerFactory) : PlayerProvider {
        override fun createPlayer(connection: PlayerConnection, gameProfile: GameProfile): Player =
            factory.create(connection, gameProfile)
    }
}
