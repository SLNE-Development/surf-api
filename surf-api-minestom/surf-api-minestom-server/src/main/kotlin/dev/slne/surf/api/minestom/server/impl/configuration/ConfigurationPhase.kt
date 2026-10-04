package dev.slne.surf.api.minestom.server.impl.configuration

import dev.slne.surf.api.minestom.server.configuration.ConfigurationContext
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTask
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTaskId
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.extension.ExceptionManager
import dev.slne.surf.api.minestom.extension.PacketListenerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.ServerFlag
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.listener.preplay.LoginListener
import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.ConnectionState
import net.minestom.server.network.packet.client.login.ClientLoginAcknowledgedPacket
import net.minestom.server.network.packet.client.play.ClientConfigurationAckPacket
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.network.player.PlayerSocketConnection
import org.jetbrains.annotations.Blocking

/**
 * Runs the configuration phase as an ordered list of [ConfigurationTask]s.
 *
 * Minestom runs its configuration in `ConnectionManager#doConfiguration`, which cannot be
 * replaced. Instead, the two packets that start a configuration - the login acknowledgement and
 * the configuration acknowledgement sent when a player is moved back from play - are routed here.
 */
internal object ConfigurationPhase {
    private val LOGGER = ComponentLogger.logger()

    private val ERROR_DURING_LOGIN = Component.text("Error during login!", NamedTextColor.RED)

    @Volatile
    private var tasks: List<Pair<ConfigurationTaskId, ConfigurationTask>> = emptyList()

    internal fun install(
        tasks: List<Pair<ConfigurationTaskId, ConfigurationTask>>,
        node: EventNode<Event>,
    ) {
        this.tasks = tasks
        tasks.forEach { (_, task) -> task.install(node) }

        PacketListenerManager.setListener(
            ConnectionState.LOGIN,
            ClientLoginAcknowledgedPacket::class.java,
            ::handleLoginAcknowledged,
        )
        PacketListenerManager.setPlayListener(ClientConfigurationAckPacket::class.java) { _, player ->
            startConfiguration(player, isFirstConfig = false)
        }
    }

    internal fun uninstall() {
        tasks = emptyList()

        PacketListenerManager.setListener(
            ConnectionState.LOGIN,
            ClientLoginAcknowledgedPacket::class.java,
            LoginListener::loginAckListener,
        )
        PacketListenerManager.setPlayListener(
            ClientConfigurationAckPacket::class.java,
            LoginListener::configAckListener,
        )
    }

    fun stopKeepAlive(player: Player) {
        ConnectionManagerAccess.keepAlivePlayers(ConnectionManager).remove(player)
    }

    private fun handleLoginAcknowledged(
        @Suppress("unused") packet: ClientLoginAcknowledgedPacket,
        connection: PlayerConnection,
    ) {
        if (connection !is PlayerSocketConnection) {
            throw UnsupportedOperationException("Only socket connections can log in")
        }

        val gameProfile = checkNotNull(connection.gameProfile()) { "Missing game profile" }

        try {
            val player = ConnectionManager.createPlayer(connection, gameProfile)
            startConfiguration(player, isFirstConfig = true)
        } catch (failure: Throwable) {
            ExceptionManager.handleException(failure)
            connection.kick(ERROR_DURING_LOGIN)
        }
    }

    /**
     * Runs the configuration on a virtual thread of its own, so that waiting for the client does
     * not hold up the thread reading its packets.
     */
    private fun startConfiguration(player: Player, isFirstConfig: Boolean) {
        Thread.startVirtualThread {
            try {
                configure(player, isFirstConfig)
            } catch (failure: Throwable) {
                ExceptionManager.handleException(failure)
                player.kick(ERROR_DURING_LOGIN)
            }
        }
    }

    @Blocking
    private fun configure(player: Player, isFirstConfig: Boolean) {
        check(ServerFlag.INSIDE_TEST || Thread.currentThread().isVirtual) {
            "The configuration must run on a virtual thread"
        }

        if (isFirstConfig) {
            ConnectionManagerAccess.configurationPlayers(ConnectionManager).add(player)
            ConnectionManagerAccess.keepAlivePlayers(ConnectionManager).add(player)

            player.refreshKeepAlive(System.nanoTime())
            player.refreshAnswerKeepAlive(true)
        }

        val context = ConfigurationContext(player, isFirstConfig)

        for ((id, task) in tasks) {
            if (!player.isOnline) return

            try {
                task.run(context)
            } catch (failure: Throwable) {
                LOGGER.error("Configuration step '{}' failed for {}", id, player.username, failure)
                throw failure
            }
        }
    }
}

/**
 * Reads the sets `ConnectionManager#doConfiguration` maintains, which Minestom keeps private.
 */
@Suppress("UNCHECKED_CAST")
private object ConnectionManagerAccess {
    private val configurationPlayers = ConnectionManager::class.java
        .getDeclaredField("configurationPlayers")
        .apply { isAccessible = true }

    private val keepAlivePlayers = ConnectionManager::class.java
        .getDeclaredField("keepAlivePlayers")
        .apply { isAccessible = true }

    fun configurationPlayers(manager: ConnectionManager): MutableSet<Player> =
        configurationPlayers.get(manager) as MutableSet<Player>

    fun keepAlivePlayers(manager: ConnectionManager): MutableSet<Player> =
        keepAlivePlayers.get(manager) as MutableSet<Player>
}
